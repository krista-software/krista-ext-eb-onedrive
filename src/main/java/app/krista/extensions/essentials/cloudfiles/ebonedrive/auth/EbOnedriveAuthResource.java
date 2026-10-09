package app.krista.extensions.essentials.cloudfiles.ebonedrive.auth;

import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.sdk.msauth.exception.MsAuthException;
import app.krista.sdk.msauth.extension.registry.ExtensionMetadata;
import app.krista.sdk.msauth.extension.registry.ExtensionRegistry;
import app.krista.sdk.msauth.token.ITokenStore;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.integration.GraphErrorTranslator;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Serves the microsoft-auth-sdk's "Authentication" tab (Pattern D) and every endpoint the shipped
 * auth/index.html JS calls into — hand-rolled, Pattern A (see adding-krista-msauth-tabs/SKILL.md's
 * Overview for why Pattern B, extending AbstractAuthenticationResource, is deprecated).
 */
@Service
@Path("/")
public class EbOnedriveAuthResource {

    private static final Logger LOG = LoggerFactory.getLogger(EbOnedriveAuthResource.class);
    private static final Gson GSON = new Gson();
    private static final String AUTH_HTML_PATH = "auth/index.html";

    private final EbOnedriveAttributeStore attributeStore;
    private final EbOnedriveAuditService auditService;
    private final ITokenStore tokenStore;
    private final String invokerId;
    private final String routingUrl;

    // A JAX-RS resource cannot @Inject bare String invokerId/routingUrl — HK2 has no binding for those
    // in this context. Inject Invoker instead and derive both from it. See Trap 0.6.
    //
    // tokenStore MUST be the shared EbOnedriveTokenStore (KeyValueStore-backed), NOT a locally
    // constructed InMemoryTokenStore — see Trap 0.11.
    @Inject
    public EbOnedriveAuthResource(EbOnedriveAttributeStore attributeStore, EbOnedriveAuditService auditService,
                                   EbOnedriveTokenStore tokenStore, Invoker invoker) {
        this.attributeStore = attributeStore;
        this.auditService = auditService;
        this.tokenStore = tokenStore;
        this.invokerId = invoker.getInvokerId();
        this.routingUrl = invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE);
    }

    /**
     * Serves the tab HTML. When Krista has NOT registered a multi-tenant Azure AD app for this
     * deployment, the Quick Setup card is hidden and the page defaults into Advanced/Your-Own-
     * Credentials mode — see Trap 0.8.
     */
    @GET
    @Path("/authentication/{subPath:.*}")
    @Produces(MediaType.TEXT_HTML)
    public Response customTabs(@PathParam("subPath") String subPath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(AUTH_HTML_PATH)) {
            if (is == null) {
                LOG.warn("Auth tab HTML not found at classpath path: {}", AUTH_HTML_PATH);
                return Response.status(Response.Status.NOT_FOUND).entity("Auth tab not found.").build();
            }
            String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            if (!EbOnedriveMsAuthAttributes.isQuickSetupAvailable()) {
                html = disableQuickSetup(html);
            }
            return Response.ok(html).build();
        } catch (IOException e) {
            throw new WebApplicationException(e);
        }
    }

    /**
     * Hides the "Quick Setup" sidebar card and forces Advanced/Your-Own-Credentials mode, without
     * touching the shipped SDK JS's source. See Trap 0.8.
     */
    private static String disableQuickSetup(String html) {
        String patch = "<script>(function(){"
                + "function hide(){var sq=document.getElementById('side-quick');if(!sq)return false;"
                + "sq.style.display='none';"
                + "if(window._a&&typeof window._a.setMode==='function')window._a.setMode('advanced');"
                + "return true}"
                + "if(hide())return;"
                + "var mo=new MutationObserver(function(){if(hide())mo.disconnect()});"
                + "mo.observe(document.body,{childList:true,subtree:true});"
                + "})();</script>";
        int bodyIdx = html.indexOf("<body>");
        if (bodyIdx < 0) {
            return html + patch;
        }
        int insertAt = bodyIdx + "<body>".length();
        return html.substring(0, insertAt) + patch + html.substring(insertAt);
    }

    @GET
    @Path("/msauth/metadata/{extensionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getExtensionMetadata(@PathParam("extensionId") String extensionId) {
        ExtensionRegistry.initialize();
        Optional<ExtensionMetadata> meta = ExtensionRegistry.get(extensionId);
        if (meta.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Extension not registered: " + extensionId + "\"}").build();
        }
        return Response.ok(meta.get().toUiJson()).build();
    }

    @GET
    @Path("callback")
    public String callbackV2(@QueryParam("code") String code, @QueryParam("state") String state,
                              @QueryParam("admin_consent") String adminConsent) {
        return handleOAuthCallback(code, state, adminConsent);
    }

    @GET
    @Path("v3/oauth/callback")
    public String callbackV3(@QueryParam("code") String code, @QueryParam("state") String state,
                              @QueryParam("admin_consent") String adminConsent) {
        return handleOAuthCallback(code, state, adminConsent);
    }

    private static final String AUTH_CONTEXT_PREFIX = "authContextId_";

    /**
     * Trap 0.10 — must load the CANDIDATE (not-yet-saved) attributes used to build the authorization
     * URL, not whatever is already permanently saved for this invoker.
     */
    private String handleOAuthCallback(String code, String state, String adminConsent) {
        if ("True".equalsIgnoreCase(adminConsent)) {
            audit(true, "OAuth Callback", "Admin consent granted", "Admin");
            return "Admin consent granted successfully! Click 'Test Connection' to verify, then 'Save' to apply.";
        }
        if (code == null || code.isBlank()) {
            audit(false, "OAuth Callback", "No authorization code received — user may have cancelled", "Unknown");
            return "Authentication Failed. Please re-authorize.";
        }
        String[] parts = state != null ? state.split("#") : new String[]{invokerId};
        String tokenKey = parts[0];
        if (tokenKey.isBlank()) {
            audit(false, "OAuth Callback", "Invalid state parameter — possible CSRF or corrupted redirect", "Unknown");
            throw new BadRequestException("Invalid OAuth state parameter.");
        }
        String authContextId = null;
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].startsWith(AUTH_CONTEXT_PREFIX)) {
                authContextId = parts[i];
                break;
            }
        }
        String triggeredBy = tokenKey.contains("_") ? tokenKey.split("_")[0] : tokenKey;
        try {
            EbOnedriveMsAuthAttributes attrs = authContextId != null ? attributeStore.load(authContextId) : null;
            if (attrs == null) {
                attrs = attributeStore.load(invokerId);
            }
            if (attrs == null) {
                audit(false, "OAuth Callback", "No credentials configured — cannot exchange code", triggeredBy);
                return "Authentication Failed. No credentials found. Please configure the extension first.";
            }
            attrs.toSdk(tokenStore).handleCallback(code, tokenKey);
            audit(true, "OAuth Callback", "User authenticated: " + triggeredBy, triggeredBy);
            return "User Authenticated Successfully. Please proceed with the request.";
        } catch (Exception e) {
            LOG.error("OAuth callback failed for invoker {}: {}", invokerId, e.getMessage(), e);
            audit(false, "OAuth Callback", e.getMessage(), triggeredBy);
            throw new WebApplicationException("Authentication failed: " + e.getMessage(), e,
                    Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            if (authContextId != null) {
                attributeStore.remove(authContextId);
            }
        }
    }

    @POST
    @Path("/saveCredentials")
    @Produces(MediaType.APPLICATION_JSON)
    public String saveCredentials(JsonObject payload) {
        auditService.startRequest();
        EbOnedriveMsAuthAttributes attrs = createAttributes(payload);
        boolean saved;
        try {
            attributeStore.save(attrs, invokerId);
            saved = true;
        } catch (Exception e) {
            LOG.error("Save Credentials failed for invoker {}: {}", invokerId, e.getMessage(), e);
            saved = false;
        }
        audit(saved, "Save Credentials",
                saved ? "Configuration saved successfully" : "Failed to save configuration", attrs.getEmail());
        return "{\"isSuccess\":" + saved + "}";
    }

    /**
     * Trap 0.5 — must fail fast with an actionable message when there is no client id anywhere, and
     * must log (SLF4J) so a failed Test Connection leaves something in the container logs.
     */
    @POST
    @Path("/testConnection")
    @Produces(MediaType.APPLICATION_JSON)
    public String testConnection(JsonObject payload) {
        auditService.startRequest();
        EbOnedriveMsAuthAttributes attrs = createAttributes(payload);
        if (isBlank(attrs.getClientId()) || isBlank(attrs.getClientSecret())) {
            String msg = "Quick Setup is not available for this extension — Krista has not "
                    + "registered a multi-tenant Azure AD app for it. Use 'Your Own Credentials' "
                    + "(Advanced) instead.";
            LOG.warn("Test Connection failed for invoker {}: {}", invokerId, msg);
            audit(false, "Test Connection", msg, attrs.getEmail());
            return jsonError(msg, null);
        }
        try {
            // getGraphClient(tokenKey), NOT getAccessToken(tokenKey) — see Trap 0.13 (getAccessToken
            // was only added in SDK 1.0.8; getGraphClient is present/identical on 1.0.3/1.0.5/1.0.8 and
            // works for every authType this attributes class can hold, including client-credentials).
            com.microsoft.graph.requests.GraphServiceClient<okhttp3.Request> graphClient =
                    attrs.toSdk(tokenStore).getGraphClient(attrs.buildTokenKey());
            // Real connectivity check against the SAME resource family this extension's real business
            // logic uses (drive/children/content — Files.Read.All) — never `.me()` (fails for
            // Private/client-credentials connections, Trap 0.14) and never a bare `.users(email)`
            // directory-object fetch (needs unrelated User.Read.All/Directory.Read.All, Trap 0.14
            // addendum). `.drive().root()` needs exactly Files.Read/Files.Read.All, the same
            // permission every real List Files/List Folders/Download request already needs.
            graphClient.users(attrs.getEmail()).drive().root().buildRequest().get();
            LOG.info("Test Connection succeeded for invoker {} (authType={})", invokerId, attrs.getAuthType());
            audit(true, "Test Connection", "Connection verified for " + attrs.getEmail(), attrs.getEmail());
            return "{\"isSuccess\":true}";
        } catch (MsAuthException e) {
            // MsAuthException (SDK's own), NOT MustAuthorizeException (platform type) — see Trap 0.13.
            String authContextId = attributeStore.save(attrs);
            String state = attrs.buildTokenKey() + "#" + authContextId
                    + (attrs.isPublicAuth() && attrs.getForwardPath() != null ? "#" + attrs.getForwardPath() : "");
            String authUrl = attrs.toSdk(tokenStore).buildAuthorizationUrl(state);
            LOG.info("Test Connection requires authorization for invoker {}, authUrl generated", invokerId);
            return jsonError(null, authUrl);
        } catch (Exception e) {
            LOG.error("Test Connection failed for invoker {} (authType={}): {}",
                    invokerId, attrs.getAuthType(), e.getMessage(), e);
            String friendly = GraphErrorTranslator.toFriendlyMessage(e);
            audit(false, "Test Connection", friendly, attrs.getEmail());
            return jsonError(friendly, null);
        }
    }

    private static String jsonError(String errorMessage, String url) {
        JsonObject o = new JsonObject();
        o.addProperty("isSuccess", false);
        if (errorMessage != null) o.addProperty("errorMessage", errorMessage);
        if (url != null) o.addProperty("url", url);
        return GSON.toJson(o);
    }

    private EbOnedriveMsAuthAttributes createAttributes(JsonObject payload) {
        String authType = payload.has("authType") && !payload.get("authType").isJsonNull()
                ? payload.get("authType").getAsString() : "Public";
        String publicAuthMode = payload.has("publicAuthMode") && !payload.get("publicAuthMode").isJsonNull()
                ? payload.get("publicAuthMode").getAsString() : "krista";
        String clientId = null, clientSecret = null, tenantId = null;
        if (!"Public".equals(authType) || "own".equals(publicAuthMode)) {
            clientId = strOrNull(payload, "clientId");
            clientSecret = strOrNull(payload, "clientSecret");
            tenantId = strOrNull(payload, "tenantId");
            if (SECRET_UNCHANGED_SENTINEL.equals(clientSecret)) {
                EbOnedriveMsAuthAttributes previous = attributeStore.load(invokerId);
                clientSecret = previous != null ? previous.getClientSecret() : null;
            }
        }
        EbOnedriveMsAuthAttributes attrs = new EbOnedriveMsAuthAttributes(clientId, clientSecret, tenantId,
                strOrNull(payload, "email"), authType, routingUrl, publicAuthMode);
        attrs.setCallbackEndPoint(resolveCallbackEndPoint(authType, publicAuthMode, routingUrl));
        attrs.setForwardPath(routingUrl + "/rest/ebonedrive/v3/oauth/callback");
        return attrs;
    }

    /** Fixed, appliance-wide path Krista registered ONCE in Azure AD for its shared, Krista-managed
     * multi-tenant Quick Setup app. Deliberately has NO per-extension/per-invoker path segment. */
    private static final String DEFAULT_CALLBACK_PATH = "/extension/api/rest/v3/oauth/callback";

    /** Trap 0.17 — the redirect_uri Microsoft validates is NOT the same URL in every auth mode. */
    private static String resolveCallbackEndPoint(String authType, String publicAuthMode, String routingUrl) {
        if ("Public".equals(authType) && !"own".equals(publicAuthMode)) {
            return routingUrl.replaceAll("/extension/api/.*", DEFAULT_CALLBACK_PATH);
        }
        return routingUrl + "/rest/ebonedrive/callback";
    }

    private static String strOrNull(JsonObject payload, String key) {
        return payload != null && payload.has(key) && !payload.get(key).isJsonNull()
                ? payload.get(key).getAsString() : null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** getCredentials secret handling — a fixed sentinel, never the stored secret in plaintext. */
    private static final String SECRET_UNCHANGED_SENTINEL = "__KRISTA_SECRET_UNCHANGED__";

    @GET
    @Path("/getCredentials")
    @Produces(MediaType.APPLICATION_JSON)
    public String getCredentials(@QueryParam("authType") String authType,
                                  @QueryParam("authFlow") String authFlow) {
        EbOnedriveMsAuthAttributes attrs = attributeStore.load(invokerId);
        if (attrs == null || authType == null) {
            return "";
        }
        if (!authType.equalsIgnoreCase(attrs.getAuthType())) {
            return "";
        }
        if ("Public".equalsIgnoreCase(authType) && authFlow != null) {
            boolean isOwnRequest = "AUTH_CODE_PRIVATE".equals(authFlow);
            boolean isOwnStored = "own".equals(attrs.getPublicAuthMode());
            if (isOwnRequest != isOwnStored) {
                return "";
            }
        }
        Map<String, Object> map = new HashMap<>(attrs.toMap());
        boolean isKristaManaged = "krista".equals(attrs.getPublicAuthMode());
        if (isKristaManaged) {
            map.remove("clientId");
            map.remove("clientSecret");
        } else if (map.get("clientSecret") != null) {
            map.put("clientSecret", SECRET_UNCHANGED_SENTINEL);
        }
        return GSON.toJson(map);
    }

    @GET
    @Path("/getAuthKey")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAuthKey() {
        EbOnedriveMsAuthAttributes attrs = attributeStore.load(invokerId);
        if (attrs == null) {
            return "";
        }
        return GSON.toJson(attrs.getAuthType() != null ? attrs.getAuthType() : "Public");
    }

    @DELETE
    @Path("/credentials")
    @Produces(MediaType.APPLICATION_JSON)
    public Response revokeCredentials() {
        try {
            attributeStore.remove(invokerId);
            return Response.ok("{\"revoked\":true}").build();
        } catch (Exception e) {
            LOG.error("Failed to revoke credentials for invoker {}: {}", invokerId, e.getMessage(), e);
            return Response.serverError().entity("{\"revoked\":false,\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    private void audit(boolean success, String catalogName, String reason, String triggeredBy) {
        try {
            auditService.record(invokerId, success ? "CATALOG_REQUEST_SUCCESS" : "CATALOG_REQUEST_FAILURE",
                    success ? "success" : "failure", catalogName, reason, null,
                    triggeredBy != null ? Map.of("triggeredBy", triggeredBy) : Map.of());
        } catch (Exception ignored) {
            // Audit failures must never break the actual auth flow.
        }
    }
}

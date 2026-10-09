package app.krista.extensions.essentials.cloudfiles.ebonedrive;

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.authorization.RequestAuthenticator;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.ChangeLog;
import app.krista.extension.impl.anno.Containerize;
import app.krista.extension.impl.anno.Extension;
import app.krista.extension.impl.anno.InvokerRequest;
import app.krista.extension.impl.anno.Java;
import app.krista.extension.impl.anno.StaticResource;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.auth.EbOnedriveAttributeStore;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.auth.EbOnedriveMsAuthAttributes;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.auth.EbOnedriveRequestAuthenticator;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.auth.EbOnedriveTokenStore;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.integration.EbOnedriveGraphClientProvider;
import app.krista.ksdk.context.AuthorizationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;

/**
 * EB Onedrive extension entry point. Pattern D (Microsoft-auth-SDK) — all credential configuration
 * happens on the custom Authentication tab (EbOnedriveAuthResource), not standard Setup-tab @Field.*
 * connection fields, so this class declares none.
 */
@Extension(name = "EB Onedrive", version = "1.0.0",
        description = "Lists files, lists folders, and downloads files from Microsoft OneDrive.",
        implementationModel = Extension.ImplementationModel.Krista_4_O)
@Containerize(baseImageVersion = "3.6.2-sp3")
@StaticResource(path = "docs", file = "docs")
@ChangeLog(file = "docs/pages/ReleaseNotes.md")
@Java(version = Java.Version.JAVA_21)
public final class EbOnedriveExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(EbOnedriveExtension.class);

    private final Invoker invoker;
    private final EbOnedriveAttributeStore attributeStore;
    private final EbOnedriveGraphClientProvider graphClientProvider;
    private final EbOnedriveRequestAuthenticator requestAuthenticator;

    @Inject
    public EbOnedriveExtension(Invoker invoker, EbOnedriveAttributeStore attributeStore,
                                EbOnedriveTokenStore tokenStore, EbOnedriveGraphClientProvider graphClientProvider,
                                AuthorizationContext authorizationContext) {
        this.invoker = invoker;
        this.attributeStore = attributeStore;
        this.graphClientProvider = graphClientProvider;
        // RequestAuthenticator is built with plain `new`, never HK2-injected — HK2 cannot bind the
        // bare String invokerId/routingUrl this class's constructor needs. See Trap 0.6 in
        // adding-krista-msauth-tabs/SKILL.md. A bad constructor here would blank EVERY custom tab,
        // not just Authentication, because getCustomTabs() has to construct this whole class first.
        String routingUrl = invoker.getRoutingInfo().getRoutingURL(
                HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE);
        this.requestAuthenticator = new EbOnedriveRequestAuthenticator(
                attributeStore, tokenStore, invoker.getInvokerId(), authorizationContext, routingUrl);
    }

    /** customTabs() must list every tab actually implemented — Authentication/Audit are served by
     * EbOnedriveApplication's JAX-RS resources; Documentation by the bundled Docsify site. No AI
     * Assistant tab — MCP is not in scope for this build. See Trap 0 in adding-krista-msauth-tabs. */
    @InvokerRequest(InvokerRequest.Type.CUSTOM_TABS)
    public Map<String, String> customTabs() {
        return Map.of(
                "Authentication", "rest/ebonedrive/authentication/",
                "Documentation", "static/docs",
                "EB Onedrive Audit Dashboard", "rest/ebonedrive/audit/");
    }

    /** Setup tab's own Test Connection button — checks the already-SAVED configuration. Distinct from
     * the Authentication tab's own Test Connection, which checks the candidate/not-yet-saved form data.
     * See Trap 0.7 in adding-krista-msauth-tabs/SKILL.md; omitting this leaves the plain Setup tab's
     * button non-functional even when the Authentication tab's own Test Connection works. */
    @InvokerRequest(InvokerRequest.Type.TEST_CONNECTION)
    public void testConnection() throws IOException {
        String invokerId = invoker.getInvokerId();
        EbOnedriveMsAuthAttributes attrs = attributeStore.load(invokerId);
        if (attrs == null || attrs.getEmail() == null || attrs.getEmail().isBlank()) {
            throw new IllegalArgumentException(
                    "OneDrive is not configured yet. Open the Authentication tab to connect an account.");
        }
        try {
            // .users(email).drive().root() — never a bare .me() (Trap 0.14) — needs exactly the
            // Files.Read.All permission every real List Files/List Folders/Download request already
            // needs, nothing broader.
            graphClientProvider.forOperator(invokerId).users(attrs.getEmail()).drive().root().buildRequest().get();
        } catch (MustAuthorizeException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Setup-tab Test Connection failed for invoker {} (authType={}): {}",
                    invokerId, attrs.getAuthType(), e.getMessage(), e);
            throw new IOException(app.krista.extensions.essentials.cloudfiles.ebonedrive.integration
                    .GraphErrorTranslator.toFriendlyMessage(e), e);
        }
    }

    /** Registers the authenticator used for real catalog-request authorization. */
    @InvokerRequest(InvokerRequest.Type.AUTHENTICATOR)
    public RequestAuthenticator getRequestAuthenticator() {
        return requestAuthenticator;
    }
}

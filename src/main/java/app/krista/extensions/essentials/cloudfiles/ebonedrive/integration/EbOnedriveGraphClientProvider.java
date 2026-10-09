package app.krista.extensions.essentials.cloudfiles.ebonedrive.integration;

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.auth.EbOnedriveAttributeStore;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.auth.EbOnedriveAuditService;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.auth.EbOnedriveMsAuthAttributes;
import app.krista.extensions.util.KeyValueStore;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import app.krista.sdk.msauth.extension.service.MsAuthGraphClientProvider;
import com.microsoft.graph.requests.GraphServiceClient;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

/**
 * Thin alias over the SDK's MsAuthGraphClientProvider. Dispatches on RequestContext#invokeAsUser() —
 * see Trap 0.9 and Trap 0.12 in adding-krista-msauth-tabs/SKILL.md. This is a single-mailbox-per-invoker
 * extension (one OneDrive connection configured once via the Authentication tab) — the operator path is
 * fully implemented; per-user delegation isn't in scope (no MCP, single-connection requirement), but the
 * invokeAsUser() dispatch check itself is still present so the failure mode if it's ever hit is a clear
 * MustAuthorizeException, not a silent wrong-mailbox bug or an NPE.
 *
 * MsAuthGraphClientProvider is NEVER @Inject-ed directly (see Trap 0.16) — it is constructed manually,
 * per call, from attrs freshly loaded from the AttributeStore.
 */
@Service
public class EbOnedriveGraphClientProvider {

    private final KeyValueStore keyValueStore;
    private final EbOnedriveAttributeStore attributeStore;
    private final RequestContext requestContext;
    private final AuthorizationContext authorizationContext;
    private final EbOnedriveAuditService auditService;

    @Inject
    public EbOnedriveGraphClientProvider(KeyValueStore keyValueStore, EbOnedriveAttributeStore attributeStore,
                                          RequestContext requestContext, AuthorizationContext authorizationContext,
                                          EbOnedriveAuditService auditService) {
        this.keyValueStore = keyValueStore;
        this.attributeStore = attributeStore;
        this.requestContext = requestContext;
        this.authorizationContext = authorizationContext;
        this.auditService = auditService;
    }

    /** Operator/admin context — the single OneDrive connection configured via the Authentication tab.
     * Used by the Setup tab's own Test Connection hook and (via forRequest below) every real request. */
    public GraphServiceClient<okhttp3.Request> forOperator(String invokerId) throws IOException {
        EbOnedriveMsAuthAttributes attrs = attributeStore.load(invokerId);
        if (attrs == null) {
            throw new MustAuthorizeException(
                    "Not configured — complete the Authentication tab first.", List.of());
        }
        MsAuthGraphClientProvider provider = new MsAuthGraphClientProvider(
                keyValueStore, attrs, requestContext, authorizationContext, invokerId);
        provider.setAuditService(auditService);
        // useSetupEmail=true — NEVER false/null here (false/null falls back to
        // AuthorizationContext#getAuthorizedAccount(), null outside a real end-user request). See Trap 0.9.
        return provider.getGraphServiceClientForUser(true, null);
    }

    /** Returns the configured mailbox's email, needed for .users(email) calls (never a bare .me()). */
    public String operatorEmail(String invokerId) {
        EbOnedriveMsAuthAttributes attrs = attributeStore.load(invokerId);
        if (attrs == null) {
            throw new MustAuthorizeException(
                    "Not configured — complete the Authentication tab first.", List.of());
        }
        return attrs.getEmail();
    }

    /** Every real catalog request routes through here, dispatching on RequestContext#invokeAsUser() —
     * see Trap 0.12. This extension is connected once, for the whole team, via the Authentication tab,
     * so per-user sign-in isn't supported — the dispatch check still exists so the failure mode is a
     * clear MustAuthorizeException rather than a silent wrong-mailbox bug. */
    public GraphServiceClient<okhttp3.Request> forRequest(RequestContext requestContext, String invokerId)
            throws IOException {
        if (!requestContext.invokeAsUser()) {
            return forOperator(invokerId);
        }
        throw new MustAuthorizeException(
                "Sign-in on behalf of this user isn't supported by this extension — it's connected once, "
                        + "for the whole team, via the Authentication tab.", List.of());
    }
}

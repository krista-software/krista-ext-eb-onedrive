package app.krista.extensions.essentials.cloudfiles.ebonedrive.auth;

import app.krista.ksdk.context.AuthorizationContext;
import app.krista.sdk.msauth.extension.base.AbstractRequestAuthenticator;
import app.krista.sdk.msauth.extension.base.MsAuthAttributes;
import app.krista.sdk.msauth.token.ITokenStore;

/**
 * Microsoft-auth-SDK request authenticator. callbackPathPrefix ("/ebonedrive") MUST match
 * EbOnedriveAuthConfig's @MsAuthExtension(callbackPathPrefix=...).
 *
 * DELIBERATELY NOT @Service/@Inject on this class or its constructor — HK2 cannot bind a bare String
 * (invokerId/routingUrl) in this container, in any context. Built with plain `new` inside
 * EbOnedriveExtension's constructor instead (which legitimately injects Invoker) — see Trap 0.6 in
 * adding-krista-msauth-tabs/SKILL.md.
 */
public class EbOnedriveRequestAuthenticator extends AbstractRequestAuthenticator {

    private final EbOnedriveAttributeStore attributeStore;
    private final ITokenStore tokenStore;
    private final String routingUrl;

    public EbOnedriveRequestAuthenticator(EbOnedriveAttributeStore attributeStore, ITokenStore tokenStore,
                                           String invokerId, AuthorizationContext authorizationContext,
                                           String routingUrl) {
        super(invokerId, routingUrl, "/ebonedrive");
        this.attributeStore = attributeStore;
        this.tokenStore = tokenStore;
        this.routingUrl = routingUrl;
    }

    @Override
    protected MsAuthAttributes loadAttributes(String key) {
        return attributeStore.load(key);
    }

    @Override
    protected ITokenStore getTokenStore() {
        // MUST be the same shared, KeyValueStore-backed store EbOnedriveAuthResource and
        // EbOnedriveGraphClientProvider use — an InMemoryTokenStore here would make a token saved
        // during the OAuth callback invisible to real catalog-request authorization. See Trap 0.11.
        return tokenStore;
    }

    @Override
    protected MsAuthAttributes createDefaultAttributes(String routingUrl) {
        return new EbOnedriveMsAuthAttributes(null, null, null, null, "Public", routingUrl, "krista");
    }
}

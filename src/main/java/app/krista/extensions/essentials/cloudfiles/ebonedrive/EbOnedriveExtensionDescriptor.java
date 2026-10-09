package app.krista.extensions.essentials.cloudfiles.ebonedrive;

import app.krista.sdk.msauth.extension.registry.MsAuthExtensionDescriptor;

/**
 * ServiceLoader descriptor that points the microsoft-auth-sdk at this extension's AuthConfig. Registered
 * in src/main/resources/META-INF/services/app.krista.sdk.msauth.extension.registry.MsAuthExtensionDescriptor.
 */
public class EbOnedriveExtensionDescriptor implements MsAuthExtensionDescriptor {
    @Override
    public Class<?> getConfigClass() {
        return EbOnedriveAuthConfig.class;
    }
}

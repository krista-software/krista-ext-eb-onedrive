package app.krista.extensions.essentials.cloudfiles.ebonedrive.catalog.support;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;

import java.util.Map;

/**
 * Builds ExtensionResponse consistently. Success wraps the field map; failure carries an
 * ExceptionType classification (AUTHENTICATION / INPUT / SYSTEM) so the platform can present the
 * right remediation.
 */
public final class ExtensionResponses {

    private ExtensionResponses() {
    }

    public static ExtensionResponse ok(Map<String, Object> values) {
        return new ExtensionResponseBuilder().success(values).build();
    }

    public static ExtensionResponse failure(String message, ExtensionResponse.Error.ExceptionType type) {
        ExtensionResponse.Error error =
                new ExtensionResponse.Error(message, System.currentTimeMillis(), type, "");
        return new ExtensionResponseBuilder().failure(error).build();
    }
}

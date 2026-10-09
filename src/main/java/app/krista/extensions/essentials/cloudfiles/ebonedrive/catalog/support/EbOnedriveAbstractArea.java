package app.krista.extensions.essentials.cloudfiles.ebonedrive.catalog.support;

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponse.Error.ExceptionType;
import app.krista.extension.executor.Invoker;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.catalog.errorhandlers.AuthorizationExceptionHandler;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.integration.GraphErrorTranslator;
import app.krista.ksdk.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared base for every EB Onedrive Area. Owns the one `audited(...)` wrapper so each
 * `@CatalogRequest` method stays thin: validate -> call the service -> transform -> build response.
 * The wrapper centralizes the try/catch -> classify -> ExtensionResponse mapping exactly once,
 * including the MustAuthorizeException dispatch required by Pattern D (Trap 0.12 in
 * adding-krista-msauth-tabs/SKILL.md).
 */
public abstract class EbOnedriveAbstractArea {

    private static final Logger LOG = LoggerFactory.getLogger(EbOnedriveAbstractArea.class);

    protected final RequestContext requestContext;
    protected final String invokerId;

    protected EbOnedriveAbstractArea(RequestContext requestContext, Invoker invoker) {
        this.requestContext = requestContext;
        this.invokerId = invoker.getInvokerId();
    }

    @FunctionalInterface
    protected interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    protected ExtensionResponse audited(String name, CheckedSupplier<ExtensionResponse> action) {
        LOG.info("Catalog '{}' started for invoker {}", name, invokerId);
        try {
            ExtensionResponse r = action.get();
            LOG.info("Catalog '{}' ok for invoker {}", name, invokerId);
            return r;
        } catch (MustAuthorizeException e) {
            return AuthorizationExceptionHandler.handleAuthorizationException(e, requestContext.invokeAsUser());
        } catch (IllegalArgumentException e) {
            LOG.warn("Catalog '{}' INPUT error for invoker {}: {}", name, invokerId, e.getMessage());
            return ExtensionResponses.failure(e.getMessage(), ExceptionType.INPUT_ERROR);
        } catch (Exception e) {
            String friendly = GraphErrorTranslator.toFriendlyMessage(e);
            LOG.error("Catalog '{}' SYSTEM error for invoker {}: {}", name, invokerId, e.getMessage(), e);
            return ExtensionResponses.failure(friendly, ExceptionType.SYSTEM_ERROR);
        }
    }

    protected static Map<String, Object> out() {
        return new LinkedHashMap<>();
    }
}

package app.krista.extensions.essentials.cloudfiles.ebonedrive.auth;

import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Set;

/**
 * Registers EbOnedriveAuthResource (Authentication tab + save/test/callback endpoints) and
 * EbOnedriveAuditResource (Audit tab) under one JAX-RS application, mounted at
 * rest/ebonedrive/... (default jaxrsId "rest", @ApplicationPath "ebonedrive" below).
 */
@Service
@ApplicationPath("ebonedrive")
@ContractsProvided(Application.class)
public class EbOnedriveApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(EbOnedriveAuthResource.class, EbOnedriveAuditResource.class);
    }
}

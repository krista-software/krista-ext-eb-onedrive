package app.krista.extensions.essentials.cloudfiles.ebonedrive.catalog;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.catalog.support.EbOnedriveAbstractArea;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.catalog.support.ExtensionResponses;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.integration.EbOnedriveGraphClientProvider;
import app.krista.ksdk.context.RequestContext;

import javax.inject.Inject;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Map;

/**
 * Setup area — the standard Test Connection + Health Check requests. Both are NEW, locally-scoped
 * requests (no match in the Essentials/Cloud Files domain catalog) — every other request in this
 * extension is a domain-reuse decision; see ManagementArea.
 */
@Domain(id = "catEntryDomain_30c63e77-37f2-4a1d-afd8-f6bfda07ad57",
        name = "Cloud Files",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "ca930cf7-ec16-49e0-84ef-bb66c80e0662")
public class SetupArea extends EbOnedriveAbstractArea {

    private final EbOnedriveGraphClientProvider graphClientProvider;

    @Inject
    public SetupArea(EbOnedriveGraphClientProvider graphClientProvider, RequestContext requestContext, Invoker invoker) {
        super(requestContext, invoker);
        this.graphClientProvider = graphClientProvider;
    }

    @CatalogRequest(
            id = "localDomainRequest_7e2a1b3c-4d5e-4f60-8a71-9b2c3d4e5f60",
            name = "Test Connection",
            description = "Validates connectivity and authentication against Microsoft OneDrive.",
            area = "Setup",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Boolean(name = "Success", required = false)
    @Field.Text(name = "Message", required = false)
    public ExtensionResponse testConnection() {
        return audited("TestConnection", () -> {
            Map<String, Object> out = out();
            // Setup tab's own Test Connection — checks the already-SAVED config (distinct from the
            // Authentication tab's own Test Connection, which checks the candidate/not-yet-saved form
            // data). See Trap 0.7 in adding-krista-msauth-tabs/SKILL.md.
            String email = graphClientProvider.operatorEmail(invokerId);
            graphClientProvider.forOperator(invokerId).users(email).drive().root().buildRequest().get();
            out.put("Success", true);
            out.put("Message", "Connected to OneDrive as " + email + ".");
            return ExtensionResponses.ok(out);
        });
    }

    @CatalogRequest(
            id = "localDomainRequest_1a2b3c4d-5e6f-4789-9abc-0def12345678",
            name = "Health Check",
            description = "Checks extension and OneDrive connectivity health.",
            area = "Setup",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Boolean(name = "Is Healthy", required = false)
    @Field(name = "Heap Used MB", type = "Number", required = false)
    @Field.Text(name = "Details", required = false)
    public ExtensionResponse healthCheck() {
        return audited("HealthCheck", () -> {
            Map<String, Object> out = out();
            MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
            long heapUsedMb = memBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
            boolean healthy;
            String details;
            try {
                String email = graphClientProvider.operatorEmail(invokerId);
                graphClientProvider.forOperator(invokerId).users(email).drive().root().buildRequest().get();
                healthy = true;
                details = "OK — connected to OneDrive as " + email + ".";
            } catch (Exception e) {
                healthy = false;
                details = "OneDrive connectivity check failed: "
                        + app.krista.extensions.essentials.cloudfiles.ebonedrive.integration.GraphErrorTranslator.toFriendlyMessage(e);
            }
            out.put("Is Healthy", healthy);
            out.put("Heap Used MB", (double) heapUsedMb);
            out.put("Details", details);
            return ExtensionResponses.ok(out);
        });
    }
}

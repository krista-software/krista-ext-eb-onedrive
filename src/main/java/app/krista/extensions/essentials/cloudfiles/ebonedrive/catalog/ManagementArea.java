package app.krista.extensions.essentials.cloudfiles.ebonedrive.catalog;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.catalog.entities.CustomFileEntity;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.catalog.entities.FileEntity;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.catalog.support.EbOnedriveAbstractArea;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.catalog.support.ExtensionResponses;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.impl.transformers.CustomFileEntityTransformer;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.impl.transformers.FileEntityTransformer;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.integration.DownloadJobResult;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.integration.EbOnedriveGraphClientProvider;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.integration.KristaMediaClient;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.integration.ListJobResult;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.integration.OneDriveFilters;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.integration.OneDriveService;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.integration.TaskResultStore;
import app.krista.extensions.util.EventHandler;
import app.krista.ksdk.context.RequestContext;
import app.krista.model.base.FreeForm;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.GraphServiceClient;

import javax.inject.Inject;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Management area — file/folder listing and download requests against Microsoft OneDrive.
 *
 * Every request here is a domain-reuse decision against the existing Essentials/Cloud Files catalog
 * (see implementation-plan.json's "reuse" blocks and plan.md's "Domain reuse" section): names, inputs,
 * and outputs match the already-published contract verbatim rather than inventing OneDrive-specific
 * shapes, so this extension's requests read identically to any other Cloud Files provider.
 *
 * <p><b>Async Task-ID pattern.</b> The reused contract for List Files / List Folders is a
 * {@code QUERY_SYSTEM} returning a Task ID, paired with a {@code WAIT_FOR_EVENT} "Get Result" that
 * returns the actual list. Microsoft Graph's own list-children call is natively synchronous, but to
 * honor the existing contract exactly as published, this class still runs it behind that two-step
 * shape: the QUERY_SYSTEM request fetches the result eagerly (following every Graph page, bounded by
 * {@link OneDriveService}'s safety cap), stores it in {@link TaskResultStore} under a fresh Task ID,
 * and fires an internal event (via the platform {@link EventHandler} — not an external webhook) whose
 * NAME embeds that Task ID; "Get Result" matches on that event name and returns the stored list.
 *
 * <p><b>Pagination note.</b> The reused "List Files"/"List Folders"/"Get Result" contract has no
 * continuation-token input or output field (the author's domain-reuse decision was to reuse these
 * verbatim), so — unlike the "one page per call, plus a continuation token" preference from this
 * build's own earlier clarification — pagination is followed internally and the full result is
 * returned in one "Get Result" call. This is a deliberate, flagged deviation forced by the reused
 * contract's shape; see build-report.json.
 *
 * <p><b>"Get the Downloaded file" has no producer in this build.</b> It's reused verbatim as a
 * standalone, correctly-implemented WAIT_FOR_EVENT consumer (it will correctly report "unknown Task
 * Id" for any input today), but none of the other nine reused requests populates its backing
 * {@link DownloadJobResult} — "Download as File" and "Download File using the File Id" return their
 * file synchronously instead, matching their own declared (non-Task-ID) output shape exactly. See
 * build-report.json for this flagged gap.
 */
@Domain(id = "catEntryDomain_30c63e77-37f2-4a1d-afd8-f6bfda07ad57",
        name = "Cloud Files",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "ca930cf7-ec16-49e0-84ef-bb66c80e0662")
public class ManagementArea extends EbOnedriveAbstractArea {

    private static final String LIST_RESULT_EVENT_PREFIX = "EBONEDRIVE_LIST_RESULT:";
    private static final String DOWNLOAD_RESULT_EVENT_PREFIX = "EBONEDRIVE_DOWNLOAD_RESULT:";

    private final OneDriveService oneDriveService;
    private final EbOnedriveGraphClientProvider graphClientProvider;
    private final TaskResultStore taskResultStore;
    private final EventHandler eventHandler;
    private final KristaMediaClient mediaClient;

    @Inject
    public ManagementArea(OneDriveService oneDriveService, EbOnedriveGraphClientProvider graphClientProvider,
                           TaskResultStore taskResultStore, EventHandler eventHandler, KristaMediaClient mediaClient,
                           RequestContext requestContext, Invoker invoker) {
        super(requestContext, invoker);
        this.oneDriveService = oneDriveService;
        this.graphClientProvider = graphClientProvider;
        this.taskResultStore = taskResultStore;
        this.eventHandler = eventHandler;
        this.mediaClient = mediaClient;
    }

    // ================================================================================================
    // List Files / List Folders — kick off the job, return a Task ID. Reused verbatim.
    // ================================================================================================

    @CatalogRequest(
            id = "localDomainRequest_f5f0a870-d4ab-4194-b673-1142f21c005a",
            name = "List Files",
            description = "Produces a non-recursive list of files that is present within the folder denoted by the source path",
            area = "Management",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Text(name = "Task ID", required = false)
    public ExtensionResponse listFiles(
            @Field.Text(name = "Source Path", required = true, attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {}) String sourcePath,
            @Field.Text(name = "Search", required = false, attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {}) String search) {
        return audited("ListFiles", () -> runListJob(sourcePath, search, false));
    }

    @CatalogRequest(
            id = "localDomainRequest_3f16a853-5d28-4af7-8d4c-6f8eab2d341a",
            name = "List Folders",
            description = "Produces a non-recursive list of folders that is present within the folder denoted by the source path",
            area = "Management",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Text(name = "Task ID", required = false)
    public ExtensionResponse listFolders(
            @Field.Text(name = "Source Path", required = true, attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {}) String sourcePath,
            @Field.Text(name = "Search", required = false, attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {}) String search) {
        return audited("ListFolders", () -> runListJob(sourcePath, search, true));
    }

    private ExtensionResponse runListJob(String sourcePath, String search, boolean foldersOnly) throws IOException {
        if (sourcePath == null || sourcePath.isBlank()) {
            throw new IllegalArgumentException("Source Path is required (use \"/\" for the drive root).");
        }
        String email = graphClientProvider.operatorEmail(invokerId);
        GraphServiceClient<okhttp3.Request> client = graphClientProvider.forOperator(invokerId);
        List<DriveItem> children = oneDriveService.listChildrenByPath(client, email, sourcePath);
        List<CustomFileEntity> items = new ArrayList<>();
        for (DriveItem item : children) {
            boolean isFolder = item.folder != null;
            if (isFolder != foldersOnly) {
                continue;
            }
            if (!OneDriveFilters.matchesSearch(item.name, search)) {
                continue;
            }
            items.add(CustomFileEntityTransformer.transform(item, sourcePath));
        }
        String taskId = "task_" + UUID.randomUUID();
        String kind = foldersOnly ? "folders" : "files";
        taskResultStore.saveListResult(taskId,
                new ListJobResult(true, "Found " + items.size() + " " + kind + ".", items));
        FreeForm eventData = new FreeForm();
        eventData.put("taskId", "Text", taskId);
        eventHandler.handleEvent(LIST_RESULT_EVENT_PREFIX + taskId, eventData);
        Map<String, Object> out = out();
        out.put("Task ID", taskId);
        return ExtensionResponses.ok(out);
    }

    // ================================================================================================
    // Get Result — the WAIT_FOR_EVENT consumer for both List Files and List Folders. Reused verbatim.
    // ================================================================================================

    @CatalogRequest(
            id = "localDomainRequest_39c4d468-6f5f-4176-a241-a01dcadad9a0",
            name = "Get Result",
            description = "Accept task ID as input and return List of Files",
            area = "Management",
            type = CatalogRequest.Type.WAIT_FOR_EVENT)
    @Field.Desc(name = "Files", type = "[ Entity(Custom File) ]", required = false)
    @Field.Boolean(name = "Success", required = false)
    @Field.Text(name = "Message", required = false)
    public ExtensionResponse getResult(
            @Field(name = "eventName", type = "Text") String eventName,
            @Field(name = "eventData", type = "FreeForm") FreeForm eventData,
            @Field.Text(name = "Task ID", required = true, attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {}) String taskId) {
        return audited("GetResult", () -> {
            if (taskId == null || taskId.isBlank()) {
                throw new IllegalArgumentException("Task ID is required.");
            }
            String expectedEventName = LIST_RESULT_EVENT_PREFIX + taskId;
            if (!expectedEventName.equals(eventName)) {
                // Not the event this call is waiting for — decline so the platform keeps waiting.
                throw new IllegalArgumentException("Event does not match the requested Task ID.");
            }
            ListJobResult result = taskResultStore.loadListResult(taskId);
            Map<String, Object> out = out();
            if (result == null) {
                out.put("Files", List.of());
                out.put("Success", false);
                out.put("Message", "Unknown or expired Task ID: " + taskId);
            } else {
                out.put("Files", result.items);
                out.put("Success", result.success);
                out.put("Message", result.message);
            }
            return ExtensionResponses.ok(out);
        });
    }

    // ================================================================================================
    // Three near-duplicate "list files in a folder" requests, reused verbatim from the existing domain.
    // ================================================================================================

    @CatalogRequest(
            id = "localDomainRequest_fcefb078-1ed1-4df5-8a48-cc82b17e39d9",
            name = "Retrieve files using folder id",
            description = "Request Description",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "Response", type = "[ Entity(File) ]", required = false)
    public ExtensionResponse retrieveFilesUsingFolderId(
            @Field.Text(name = "Folder Id", required = true, attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {}) String folderId,
            @Field.Date(name = "From Date", required = false, attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {}) Long fromDate) {
        return audited("RetrieveFilesUsingFolderId", () -> {
            if (folderId == null || folderId.isBlank()) {
                throw new IllegalArgumentException("Folder Id is required.");
            }
            String email = graphClientProvider.operatorEmail(invokerId);
            GraphServiceClient<okhttp3.Request> client = graphClientProvider.forOperator(invokerId);
            List<DriveItem> children = oneDriveService.listChildrenById(client, email, folderId);
            List<FileEntity> files = filterAndTransformFiles(children, fromDate, null);
            Map<String, Object> out = out();
            out.put("Response", files);
            return ExtensionResponses.ok(out);
        });
    }

    @CatalogRequest(
            id = "localDomainRequest_a5e85205-cc78-4107-ab93-e7468d6c0bdc",
            name = "Get files and File Id from Folder Id",
            description = "Request Description",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "List of Files", type = "[ Entity(File) ]", required = false)
    public ExtensionResponse getFilesAndFileIdFromFolderId(
            @Field.Text(name = "Folder Id", required = true, attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {}) String folderId,
            @Field.Date(name = "From Date", required = false, attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {}) Long fromDate) {
        return audited("GetFilesAndFileIdFromFolderId", () -> {
            if (folderId == null || folderId.isBlank()) {
                throw new IllegalArgumentException("Folder Id is required.");
            }
            String email = graphClientProvider.operatorEmail(invokerId);
            GraphServiceClient<okhttp3.Request> client = graphClientProvider.forOperator(invokerId);
            List<DriveItem> children = oneDriveService.listChildrenById(client, email, folderId);
            List<FileEntity> files = filterAndTransformFiles(children, fromDate, null);
            Map<String, Object> out = out();
            out.put("List of Files", files);
            return ExtensionResponses.ok(out);
        });
    }

    @CatalogRequest(
            id = "localDomainRequest_7f15df55-3300-4dda-8a0d-5ce6d1f64712",
            name = "Fetch Files From Folder",
            description = "Request Description",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "Response", type = "[ Entity(File) ]", required = false)
    public ExtensionResponse fetchFilesFromFolder(
            @Field.Text(name = "Path", required = true, attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {}) String path,
            @Field.Date(name = "From Date", required = false, attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {}) Long fromDate) {
        return audited("FetchFilesFromFolder", () -> {
            if (path == null || path.isBlank()) {
                throw new IllegalArgumentException("Path is required (use \"/\" for the drive root).");
            }
            String email = graphClientProvider.operatorEmail(invokerId);
            GraphServiceClient<okhttp3.Request> client = graphClientProvider.forOperator(invokerId);
            List<DriveItem> children = oneDriveService.listChildrenByPath(client, email, path);
            List<FileEntity> files = filterAndTransformFiles(children, fromDate, path);
            Map<String, Object> out = out();
            out.put("Response", files);
            return ExtensionResponses.ok(out);
        });
    }

    private List<FileEntity> filterAndTransformFiles(List<DriveItem> children, Long fromDate, String parentPath) {
        List<FileEntity> files = new ArrayList<>();
        for (DriveItem item : children) {
            if (item.folder != null) {
                continue;
            }
            Long modified = app.krista.extensions.essentials.cloudfiles.ebonedrive.impl.transformers
                    .CustomFileEntityTransformer.toEpochMillis(item.lastModifiedDateTime);
            if (!OneDriveFilters.isOnOrAfter(modified, fromDate)) {
                continue;
            }
            files.add(FileEntityTransformer.transform(item, parentPath));
        }
        return files;
    }

    // ================================================================================================
    // Get Folder Id by path — reused verbatim.
    // ================================================================================================

    @CatalogRequest(
            id = "localDomainRequest_3513c211-3ee6-4ae5-84c3-91a3d6e58dd9",
            name = "Get Folder Id by path",
            description = "Request Description",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Text(name = "Folder Id", required = false)
    public ExtensionResponse getFolderIdByPath(
            @Field.Text(name = "Path", required = true, attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {}) String path) {
        return audited("GetFolderIdByPath", () -> {
            if (path == null || path.isBlank()) {
                throw new IllegalArgumentException("Path is required.");
            }
            String email = graphClientProvider.operatorEmail(invokerId);
            GraphServiceClient<okhttp3.Request> client = graphClientProvider.forOperator(invokerId);
            DriveItem folder = oneDriveService.getFolderByPath(client, email, path);
            Map<String, Object> out = out();
            out.put("Folder Id", folder != null ? folder.id : null);
            return ExtensionResponses.ok(out);
        });
    }

    // ================================================================================================
    // Download requests — reused verbatim. Both are synchronous and return the binary file directly,
    // matching their declared (non-Task-ID) output shape exactly.
    // ================================================================================================

    @CatalogRequest(
            id = "localDomainRequest_d0aea3ac-2059-4ce4-85e8-626b42e64c0c",
            name = "Download as File",
            description = "Retrieves the File from the cloud storage and provides a reference as Krista File based on provided source location.",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.File(name = "Krista File", required = false, attributes = {}, options = {})
    @Field.Boolean(name = "Success", required = false)
    @Field(name = "Message", type = "RichText", required = false)
    public ExtensionResponse downloadAsFile(
            @Field.Text(name = "Source", required = true, attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {}) String source) {
        return audited("DownloadAsFile", () -> {
            if (source == null || source.isBlank()) {
                throw new IllegalArgumentException("Source is required.");
            }
            String email = graphClientProvider.operatorEmail(invokerId);
            GraphServiceClient<okhttp3.Request> client = graphClientProvider.forOperator(invokerId);
            DriveItem item = oneDriveService.getFileByPath(client, email, source);
            Map<String, Object> out = out();
            try (InputStream content = oneDriveService.downloadContentByPath(client, email, source)) {
                java.io.File local = downloadToTempFile(content, item != null ? item.name : null);
                out.put("Krista File", mediaClient.toKristaFile(local));
                out.put("Success", true);
                out.put("Message", "Downloaded successfully.");
            }
            return ExtensionResponses.ok(out);
        });
    }

    @CatalogRequest(
            id = "localDomainRequest_4e95bf64-d4b8-40e9-9ae6-836b9a71a48b",
            name = "Download File using the File Id",
            description = "Request Description",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.File(name = "File", required = false, attributes = {}, options = {})
    public ExtensionResponse downloadFileUsingTheFileId(
            @Field.Text(name = "File Id", required = true, attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {}) String fileId) {
        return audited("DownloadFileUsingTheFileId", () -> {
            if (fileId == null || fileId.isBlank()) {
                throw new IllegalArgumentException("File Id is required.");
            }
            String email = graphClientProvider.operatorEmail(invokerId);
            GraphServiceClient<okhttp3.Request> client = graphClientProvider.forOperator(invokerId);
            DriveItem item = oneDriveService.getItemById(client, email, fileId);
            Map<String, Object> out = out();
            try (InputStream content = oneDriveService.downloadContentById(client, email, fileId)) {
                java.io.File local = downloadToTempFile(content, item != null ? item.name : null);
                out.put("File", mediaClient.toKristaFile(local));
            }
            return ExtensionResponses.ok(out);
        });
    }

    // ================================================================================================
    // Get the Downloaded file — reused verbatim. See this class's Javadoc: no producer in this build.
    // ================================================================================================

    @CatalogRequest(
            id = "localDomainRequest_a151c209-1326-43fe-aa78-acca7a0bbfa0",
            name = "Get the Downloaded file",
            description = "Get the downloaded file",
            area = "Management",
            type = CatalogRequest.Type.WAIT_FOR_EVENT)
    @Field.File(name = "File", required = false, attributes = {}, options = {})
    @Field.Boolean(name = "Success", required = false)
    @Field.Text(name = "Message", required = false)
    public ExtensionResponse getTheDownloadedFile(
            @Field(name = "eventName", type = "Text") String eventName,
            @Field(name = "eventData", type = "FreeForm") FreeForm eventData,
            @Field.Text(name = "Task Id", required = true, attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {}) String taskId) {
        return audited("GetTheDownloadedFile", () -> {
            if (taskId == null || taskId.isBlank()) {
                throw new IllegalArgumentException("Task Id is required.");
            }
            String expectedEventName = DOWNLOAD_RESULT_EVENT_PREFIX + taskId;
            if (!expectedEventName.equals(eventName)) {
                throw new IllegalArgumentException("Event does not match the requested Task Id.");
            }
            DownloadJobResult result = taskResultStore.loadDownloadResult(taskId);
            Map<String, Object> out = out();
            if (result == null) {
                out.put("File", null);
                out.put("Success", false);
                out.put("Message", "Unknown or expired Task Id: " + taskId);
            } else {
                out.put("File", result.file);
                out.put("Success", result.success);
                out.put("Message", result.message);
            }
            return ExtensionResponses.ok(out);
        });
    }

    private static java.io.File downloadToTempFile(InputStream content, String fileName) throws IOException {
        java.io.File dir = java.nio.file.Files.createTempDirectory("ebonedrive-").toFile();
        String name = (fileName != null && !fileName.isBlank()) ? fileName : "download";
        java.io.File file = new java.io.File(dir, name);
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = content.read(buf)) != -1) {
                os.write(buf, 0, n);
            }
        }
        return file;
    }
}

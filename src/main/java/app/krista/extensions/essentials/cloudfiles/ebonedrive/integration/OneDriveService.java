package app.krista.extensions.essentials.cloudfiles.ebonedrive.integration;

import com.microsoft.graph.requests.DriveItemCollectionPage;
import com.microsoft.graph.requests.DriveItemCollectionRequestBuilder;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.models.DriveItem;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Business logic against Microsoft Graph's OneDrive ("drive") resource. Every call goes through
 * `.users(email)` (never a bare `.me()` — Trap 0.14 in adding-krista-msauth-tabs/SKILL.md), since this
 * extension's connection may be either delegated (Public) or app-only/client-credentials (Private).
 *
 * Pagination: Graph pages drive-children at up to 200 items per call. The reused Cloud Files domain
 * contract for List Files/List Folders/Get Result has no continuation-token field (see
 * ManagementArea's class Javadoc for the full reasoning), so this class follows every page internally,
 * bounded by MAX_PAGES/MAX_ITEMS as a safety cap against a pathologically large folder.
 */
@Service
public class OneDriveService {

    private static final Logger LOG = LoggerFactory.getLogger(OneDriveService.class);
    private static final int MAX_PAGES = 25;
    private static final int MAX_ITEMS = 5000;

    /** Resolves a folder identified by path (blank/"/" = drive root) to its DriveItem metadata. */
    public DriveItem getFolderByPath(GraphServiceClient<okhttp3.Request> client, String email, String path) {
        if (OneDriveFilters.isRoot(path)) {
            return client.users(email).drive().root().buildRequest().get();
        }
        return client.users(email).drive().root().itemWithPath(encodePath(path)).buildRequest().get();
    }

    /** Resolves an item (file or folder) identified by its Graph drive-item id. */
    public DriveItem getItemById(GraphServiceClient<okhttp3.Request> client, String email, String itemId) {
        return client.users(email).drive().items(itemId).buildRequest().get();
    }

    /** Resolves a file identified by path to its DriveItem metadata. */
    public DriveItem getFileByPath(GraphServiceClient<okhttp3.Request> client, String email, String path) {
        return client.users(email).drive().root().itemWithPath(encodePath(path)).buildRequest().get();
    }

    /** Lists every child (file and folder) of the folder identified by path, following all pages. */
    public List<DriveItem> listChildrenByPath(GraphServiceClient<okhttp3.Request> client, String email, String path) {
        DriveItemCollectionPage page = OneDriveFilters.isRoot(path)
                ? client.users(email).drive().root().children().buildRequest().get()
                : client.users(email).drive().root().itemWithPath(encodePath(path)).children().buildRequest().get();
        return collectAllPages(page);
    }

    /** Lists every child (file and folder) of the folder identified by its Graph drive-item id. */
    public List<DriveItem> listChildrenById(GraphServiceClient<okhttp3.Request> client, String email, String folderId) {
        DriveItemCollectionPage page = client.users(email).drive().items(folderId).children().buildRequest().get();
        return collectAllPages(page);
    }

    /** Streams a file's content, identified by path. Caller is responsible for closing the stream. */
    public InputStream downloadContentByPath(GraphServiceClient<okhttp3.Request> client, String email, String path) throws IOException {
        return client.users(email).drive().root().itemWithPath(encodePath(path)).content().buildRequest().get();
    }

    /** Streams a file's content, identified by its Graph drive-item id. */
    public InputStream downloadContentById(GraphServiceClient<okhttp3.Request> client, String email, String itemId) throws IOException {
        return client.users(email).drive().items(itemId).content().buildRequest().get();
    }

    private List<DriveItem> collectAllPages(DriveItemCollectionPage firstPage) {
        List<DriveItem> all = new ArrayList<>();
        DriveItemCollectionPage page = firstPage;
        int pages = 0;
        while (page != null && pages < MAX_PAGES && all.size() < MAX_ITEMS) {
            List<DriveItem> current = page.getCurrentPage();
            if (current != null) {
                all.addAll(current);
            }
            DriveItemCollectionRequestBuilder next = page.getNextPage();
            page = next != null ? next.buildRequest().get() : null;
            pages++;
        }
        if (page != null) {
            LOG.warn("Stopped following OneDrive children pages after {} pages / {} items — folder has more "
                    + "content than this safety cap allows.", pages, all.size());
        }
        return all;
    }

    private static String encodePath(String path) {
        return OneDriveFilters.encodePathSegments(path);
    }
}

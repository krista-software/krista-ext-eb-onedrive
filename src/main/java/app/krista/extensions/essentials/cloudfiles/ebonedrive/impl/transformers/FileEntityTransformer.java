package app.krista.extensions.essentials.cloudfiles.ebonedrive.impl.transformers;

import app.krista.extensions.essentials.cloudfiles.ebonedrive.catalog.entities.FileEntity;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.integration.OneDriveFilters;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.IdentitySet;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps a Microsoft Graph DriveItem (a file — no `folder` facet) to the File entity used by the
 * folder-id/path file-listing requests and the download requests. null-in -> null-out.
 */
public final class FileEntityTransformer {

    private FileEntityTransformer() {
    }

    public static FileEntity transform(DriveItem item, String parentFolderPath) {
        if (item == null) {
            return null;
        }
        FileEntity e = new FileEntity();
        e.name = item.name;
        e.baseName = OneDriveFilters.baseName(item.name);
        e.extension = OneDriveFilters.extension(item.name);
        e.parentFolder = parentFolderPath;
        e.absolutePath = OneDriveFilters.joinPath(parentFolderPath, item.name);
        e.owner = displayName(item.createdBy);
        e.created = CustomFileEntityTransformer.toEpochMillis(item.createdDateTime);
        e.modified = CustomFileEntityTransformer.toEpochMillis(item.lastModifiedDateTime);
        e.size = item.size != null ? item.size.doubleValue() : null;
        // Permissions needs a separate Graph call (not returned by a children/content listing) — left
        // null; see the field's own description.
        e.permissions = null;
        e.isFolder = Boolean.FALSE;
        e.tags = null;
        e.sharedWith = null;
        e.createdBy = displayName(item.createdBy);
        e.modifiedBy = displayName(item.lastModifiedBy);
        e.file = null; // populated separately by the download requests once content is fetched
        return e;
    }

    public static List<FileEntity> transformList(List<DriveItem> items, String parentFolderPath) {
        List<FileEntity> out = new ArrayList<>();
        if (items == null) {
            return out;
        }
        for (DriveItem item : items) {
            // Only files belong in a File-entity list — a folder facet means this item isn't a file.
            if (item != null && item.folder == null) {
                FileEntity e = transform(item, parentFolderPath);
                if (e != null) {
                    out.add(e);
                }
            }
        }
        return out;
    }

    private static String displayName(IdentitySet identitySet) {
        if (identitySet == null || identitySet.user == null) {
            return null;
        }
        return identitySet.user.displayName;
    }
}

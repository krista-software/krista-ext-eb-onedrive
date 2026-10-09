package app.krista.extensions.essentials.cloudfiles.ebonedrive.impl.transformers;

import app.krista.extensions.essentials.cloudfiles.ebonedrive.catalog.entities.CustomFileEntity;
import app.krista.extensions.essentials.cloudfiles.ebonedrive.integration.OneDriveFilters;
import com.microsoft.graph.models.DriveItem;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps a Microsoft Graph DriveItem to the generic Custom File entity used by the List Files / List
 * Folders async job result (ManagementArea#getResult). null-in -> null-out; a DriveItem with no
 * `folder` facet is treated as a file.
 */
public final class CustomFileEntityTransformer {

    private CustomFileEntityTransformer() {
    }

    public static CustomFileEntity transform(DriveItem item, String parentFolderPath) {
        if (item == null) {
            return null;
        }
        CustomFileEntity e = new CustomFileEntity();
        boolean isFolder = item.folder != null;
        e.name = item.name;
        e.baseName = isFolder ? item.name : OneDriveFilters.baseName(item.name);
        e.extension = isFolder ? "" : OneDriveFilters.extension(item.name);
        e.fileId = item.id;
        e.created = toEpochMillis(item.createdDateTime);
        e.size = item.size != null ? item.size.doubleValue() : null;
        e.absolutePath = OneDriveFilters.joinPath(parentFolderPath, item.name);
        e.parentFolder = parentFolderPath;
        e.isFolder = isFolder;
        return e;
    }

    public static List<CustomFileEntity> transformList(List<DriveItem> items, String parentFolderPath) {
        List<CustomFileEntity> out = new ArrayList<>();
        if (items == null) {
            return out;
        }
        for (DriveItem item : items) {
            CustomFileEntity e = transform(item, parentFolderPath);
            if (e != null) {
                out.add(e);
            }
        }
        return out;
    }

    public static Long toEpochMillis(OffsetDateTime dateTime) {
        return dateTime != null ? dateTime.toInstant().toEpochMilli() : null;
    }
}

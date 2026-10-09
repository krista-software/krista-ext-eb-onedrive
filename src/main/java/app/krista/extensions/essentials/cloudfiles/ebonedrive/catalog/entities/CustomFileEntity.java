package app.krista.extensions.essentials.cloudfiles.ebonedrive.catalog.entities;

import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Entity;
import app.krista.extension.impl.anno.Field;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * "Custom File" — the generic, mixed file-or-folder item returned by the async List Files / List
 * Folders job (see ManagementArea#getResult). Reused verbatim (field shape + id) from the existing
 * Essentials/Cloud Files domain entity localDomainEntity_3e0f56dd-ecad-437f-8c8e-5c3d4e3e502c — the
 * "Is Folder" flag is what lets one shared list represent both files and folders.
 */
@Domain(id = "catEntryDomain_30c63e77-37f2-4a1d-afd8-f6bfda07ad57",
        name = "Cloud Files",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "ca930cf7-ec16-49e0-84ef-bb66c80e0662")
@Entity(name = "Custom File",
        id = "localDomainEntity_3e0f56dd-ecad-437f-8c8e-5c3d4e3e502c",
        primaryKey = "File Id",
        supportStore = false,
        description = "A file or folder item in OneDrive, as returned by the List Files / List Folders "
                + "job result. 'Is Folder' distinguishes a folder entry from a file entry.")
public class CustomFileEntity {

    @Field.Text(name = "Name", required = false, description = "The item's display name.")
    public String name;

    @Field.Text(name = "Base Name", required = false, description = "The name without its file extension.")
    public String baseName;

    @Field.Text(name = "Extension", required = false, description = "The file extension, without the dot. Empty for folders.")
    public String extension;

    @Field.Text(name = "File Id", required = false, description = "The OneDrive drive-item id.")
    public String fileId;

    @Field.Date(name = "Created", required = false, includeTimeOfDay = true,
            allowPast = true, allowToday = true, allowFuture = false,
            description = "When the item was created in OneDrive.")
    public Long created;

    @Field(name = "Size", type = "Number", required = false, description = "Size in bytes (0 for folders).")
    public Double size;

    @Field.Text(name = "Absolute Path", required = false, description = "The item's full path within the drive.")
    public String absolutePath;

    @Field.Text(name = "Parent Folder", required = false, description = "The path of the containing folder.")
    public String parentFolder;

    @Field.Boolean(name = "Is Folder", required = false, description = "True when this item is a folder, false when it is a file.")
    public Boolean isFolder;

    public Map<String, Object> toFields() {
        Map<String, Object> fieldMap = new LinkedHashMap<>();
        fieldMap.put("Name", name);
        fieldMap.put("Base Name", baseName);
        fieldMap.put("Extension", extension);
        fieldMap.put("File Id", fileId);
        fieldMap.put("Created", created);
        fieldMap.put("Size", size);
        fieldMap.put("Absolute Path", absolutePath);
        fieldMap.put("Parent Folder", parentFolder);
        fieldMap.put("Is Folder", isFolder);
        return fieldMap;
    }
}

package app.krista.extensions.essentials.cloudfiles.ebonedrive.catalog.entities;

import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Entity;
import app.krista.extension.impl.anno.Field;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "File" — a single OneDrive file. Reused verbatim (field shape + id) from the existing
 * Essentials/Cloud Files domain entity localDomainEntity_292c2a1d-0d6c-4307-a4d1-04176dd5a315.
 *
 * The reused shape has no dedicated id field, so Absolute Path (unique within a drive) is used as the
 * primary key.
 */
@Domain(id = "catEntryDomain_30c63e77-37f2-4a1d-afd8-f6bfda07ad57",
        name = "Cloud Files",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "ca930cf7-ec16-49e0-84ef-bb66c80e0662")
@Entity(name = "File",
        id = "localDomainEntity_292c2a1d-0d6c-4307-a4d1-04176dd5a315",
        primaryKey = "Absolute Path",
        supportStore = false,
        description = "A single file in OneDrive, returned by the file-listing and download requests.")
public class FileEntity {

    @Field.Text(name = "Name", required = false, description = "The file's display name.")
    public String name;

    @Field.Text(name = "Base Name", required = false, description = "The name without its file extension.")
    public String baseName;

    @Field.Text(name = "Extension", required = false, description = "The file extension, without the dot.")
    public String extension;

    @Field.Text(name = "Parent Folder", required = false, description = "The path of the containing folder.")
    public String parentFolder;

    @Field.Text(name = "Absolute Path", required = false, description = "The file's full path within the drive.")
    public String absolutePath;

    @Field.Text(name = "Owner", required = false, description = "Display name of the file's owner, when known.")
    public String owner;

    @Field.Date(name = "Created", required = false, includeTimeOfDay = true,
            allowPast = true, allowToday = true, allowFuture = false, description = "When the file was created.")
    public Long created;

    @Field.Date(name = "Modified", required = false, includeTimeOfDay = true,
            allowPast = true, allowToday = true, allowFuture = false, description = "When the file was last modified.")
    public Long modified;

    @Field(name = "Size", type = "Number", required = false, description = "Size in bytes.")
    public Double size;

    @Field.Text(name = "Permissions", required = false,
            description = "Not populated by Graph's drive-children listing (would require a separate "
                    + "permissions call); reserved for a future permissions-lookup request.")
    public String permissions;

    @Field.Boolean(name = "Is Folder", required = false, description = "Always false for a File entity.")
    public Boolean isFolder;

    @Field.Desc(name = "Tags", type = "[ Text ]", required = false,
            description = "Not populated — OneDrive drive items don't carry a tags concept.")
    public List<String> tags;

    @Field.Desc(name = "Shared With", type = "[ Text ]", required = false,
            description = "Not populated by the drive-children listing; reserved for a future sharing-lookup request.")
    public List<String> sharedWith;

    @Field.Text(name = "Created By", required = false, description = "Display name of who created the file.")
    public String createdBy;

    @Field.Text(name = "Modified By", required = false, description = "Display name of who last modified the file.")
    public String modifiedBy;

    @Field.File(name = "File", required = false, description = "The downloaded file content, populated by the download requests.")
    public app.krista.model.base.File file;

    public Map<String, Object> toFields() {
        Map<String, Object> fieldMap = new LinkedHashMap<>();
        fieldMap.put("Name", name);
        fieldMap.put("Base Name", baseName);
        fieldMap.put("Extension", extension);
        fieldMap.put("Parent Folder", parentFolder);
        fieldMap.put("Absolute Path", absolutePath);
        fieldMap.put("Owner", owner);
        fieldMap.put("Created", created);
        fieldMap.put("Modified", modified);
        fieldMap.put("Size", size);
        fieldMap.put("Permissions", permissions);
        fieldMap.put("Is Folder", isFolder);
        fieldMap.put("Tags", tags);
        fieldMap.put("Shared With", sharedWith);
        fieldMap.put("Created By", createdBy);
        fieldMap.put("Modified By", modifiedBy);
        fieldMap.put("File", file);
        return fieldMap;
    }
}

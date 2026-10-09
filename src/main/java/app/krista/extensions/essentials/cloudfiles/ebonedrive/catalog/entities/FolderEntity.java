package app.krista.extensions.essentials.cloudfiles.ebonedrive.catalog.entities;

import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Entity;
import app.krista.extension.impl.anno.Field;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "Folder" — reused verbatim (field shape + id) from the existing Essentials/Cloud Files domain entity
 * localDomainEntity_f6abaeea-5400-4cc7-9ec4-e884e723620c, per the author's domain-reuse review.
 *
 * None of this build's ten in-scope requests produces a standalone Folder entity today (List Folders'
 * results come back through the shared Custom File shape — see ManagementArea#getResult) — this class
 * is registered for the domain so a future folder-detail request in this or another Cloud Files
 * extension can reuse it rather than inventing a new one, exactly as this build reused it.
 */
@Domain(id = "catEntryDomain_30c63e77-37f2-4a1d-afd8-f6bfda07ad57",
        name = "Cloud Files",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "ca930cf7-ec16-49e0-84ef-bb66c80e0662")
@Entity(name = "Folder",
        id = "localDomainEntity_f6abaeea-5400-4cc7-9ec4-e884e723620c",
        primaryKey = "Folder ID",
        supportStore = false,
        description = "A folder in OneDrive.")
public class FolderEntity {

    @Field.Text(name = "Name", required = false)
    public String name;

    @Field.Text(name = "Base Name", required = false)
    public String baseName;

    @Field.Text(name = "Extension", required = false)
    public String extension;

    @Field.Text(name = "Parent Folder", required = false)
    public String parentFolder;

    @Field.Text(name = "Absolute Path", required = false)
    public String absolutePath;

    @Field.Text(name = "Owner", required = false)
    public String owner;

    @Field.Date(name = "Created", required = false, includeTimeOfDay = true,
            allowPast = true, allowToday = true, allowFuture = false)
    public Long created;

    @Field.Date(name = "Modified", required = false, includeTimeOfDay = true,
            allowPast = true, allowToday = true, allowFuture = false)
    public Long modified;

    @Field(name = "Size", type = "Number", required = false)
    public Double size;

    @Field.Text(name = "Permissions", required = false)
    public String permissions;

    @Field.Boolean(name = "Is Folder", required = false)
    public Boolean isFolder;

    @Field.Text(name = "Folder ID", required = false)
    public String folderId;

    @Field.Desc(name = "Tags", type = "[ Text ]", required = false)
    public List<String> tags;

    @Field.Text(name = "Created By", required = false)
    public String createdBy;

    @Field.Text(name = "Modified By", required = false)
    public String modifiedBy;

    @Field.File(name = "File", required = false)
    public app.krista.model.base.File file;

    @Field.Desc(name = "Shared With", type = "[ Text ]", required = false)
    public List<String> sharedWith;

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
        fieldMap.put("Folder ID", folderId);
        fieldMap.put("Tags", tags);
        fieldMap.put("Created By", createdBy);
        fieldMap.put("Modified By", modifiedBy);
        fieldMap.put("File", file);
        fieldMap.put("Shared With", sharedWith);
        return fieldMap;
    }
}

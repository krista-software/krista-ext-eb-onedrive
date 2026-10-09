package app.krista.extensions.essentials.cloudfiles.ebonedrive.impl.transformers;

import app.krista.extensions.essentials.cloudfiles.ebonedrive.catalog.entities.FileEntity;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.Folder;
import com.microsoft.graph.models.Identity;
import com.microsoft.graph.models.IdentitySet;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileEntityTransformerTest {

    @Test
    void transform_nullInputReturnsNull() {
        assertNull(FileEntityTransformer.transform(null, "/Reports"));
    }

    @Test
    void transform_mapsFieldsAndOwnerFromCreatedBy() {
        DriveItem item = new DriveItem();
        item.name = "budget.xlsx";
        item.size = 4096L;
        item.createdDateTime = OffsetDateTime.parse("2024-01-01T00:00:00Z");
        item.lastModifiedDateTime = OffsetDateTime.parse("2024-02-01T00:00:00Z");
        IdentitySet createdBy = new IdentitySet();
        Identity user = new Identity();
        user.displayName = "Jane Doe";
        createdBy.user = user;
        item.createdBy = createdBy;

        FileEntity e = FileEntityTransformer.transform(item, "/Reports");

        assertEquals("budget.xlsx", e.name);
        assertEquals("budget", e.baseName);
        assertEquals("xlsx", e.extension);
        assertEquals("/Reports/budget.xlsx", e.absolutePath);
        assertEquals("/Reports", e.parentFolder);
        assertEquals("Jane Doe", e.owner);
        assertEquals("Jane Doe", e.createdBy);
        assertEquals(4096.0, e.size);
        assertFalse(e.isFolder);
        assertNull(e.file);
    }

    @Test
    void transform_missingIdentityDoesNotThrow() {
        DriveItem item = new DriveItem();
        item.name = "no-owner.txt";

        FileEntity e = FileEntityTransformer.transform(item, "/");

        assertNull(e.owner);
        assertNull(e.createdBy);
        assertNull(e.modifiedBy);
    }

    @Test
    void transformList_skipsFoldersKeepsFiles() {
        DriveItem file = new DriveItem();
        file.name = "a.txt";
        DriveItem folder = new DriveItem();
        folder.name = "Sub";
        folder.folder = new Folder();
        List<DriveItem> items = Arrays.asList(file, folder);

        List<FileEntity> out = FileEntityTransformer.transformList(items, "/");

        assertEquals(1, out.size());
        assertEquals("a.txt", out.get(0).name);
    }

    @Test
    void transformList_nullReturnsEmptyList() {
        assertTrue(FileEntityTransformer.transformList(null, "/").isEmpty());
    }
}

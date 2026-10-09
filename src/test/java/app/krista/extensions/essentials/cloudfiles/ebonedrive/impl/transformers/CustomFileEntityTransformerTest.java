package app.krista.extensions.essentials.cloudfiles.ebonedrive.impl.transformers;

import app.krista.extensions.essentials.cloudfiles.ebonedrive.catalog.entities.CustomFileEntity;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.Folder;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CustomFileEntityTransformerTest {

    @Test
    void transform_nullInputReturnsNull() {
        assertNull(CustomFileEntityTransformer.transform(null, "/Reports"));
    }

    @Test
    void transform_fileItem_mapsAllFields() {
        DriveItem item = new DriveItem();
        item.id = "item-123";
        item.name = "report.pdf";
        item.size = 2048L;
        item.createdDateTime = OffsetDateTime.parse("2024-01-15T10:00:00Z");

        CustomFileEntity e = CustomFileEntityTransformer.transform(item, "/Reports");

        assertEquals("report.pdf", e.name);
        assertEquals("report", e.baseName);
        assertEquals("pdf", e.extension);
        assertEquals("item-123", e.fileId);
        assertEquals(2048.0, e.size);
        assertEquals("/Reports/report.pdf", e.absolutePath);
        assertEquals("/Reports", e.parentFolder);
        assertFalse(e.isFolder);
        assertEquals(OffsetDateTime.parse("2024-01-15T10:00:00Z").toInstant().toEpochMilli(), e.created);
    }

    @Test
    void transform_folderItem_isFolderTrueAndNoExtension() {
        DriveItem item = new DriveItem();
        item.id = "folder-1";
        item.name = "Invoices";
        item.folder = new Folder();

        CustomFileEntity e = CustomFileEntityTransformer.transform(item, "/");

        assertTrue(e.isFolder);
        assertEquals("", e.extension);
        assertEquals("Invoices", e.baseName);
        assertEquals("/Invoices", e.absolutePath);
    }

    @Test
    void transformList_nullReturnsEmptyList() {
        assertTrue(CustomFileEntityTransformer.transformList(null, "/").isEmpty());
    }

    @Test
    void transformList_mapsEveryItemAndPreservesOrder() {
        DriveItem a = new DriveItem();
        a.id = "a";
        a.name = "a.txt";
        DriveItem b = new DriveItem();
        b.id = "b";
        b.name = "b.txt";
        List<DriveItem> items = Arrays.asList(a, b);

        List<CustomFileEntity> out = CustomFileEntityTransformer.transformList(items, "/");

        assertEquals(2, out.size());
        assertEquals("a.txt", out.get(0).name);
        assertEquals("b.txt", out.get(1).name);
    }

    @Test
    void transform_nullSizeAndDateAreNull() {
        DriveItem item = new DriveItem();
        item.id = "x";
        item.name = "x.txt";

        CustomFileEntity e = CustomFileEntityTransformer.transform(item, "/");

        assertNull(e.size);
        assertNull(e.created);
    }
}

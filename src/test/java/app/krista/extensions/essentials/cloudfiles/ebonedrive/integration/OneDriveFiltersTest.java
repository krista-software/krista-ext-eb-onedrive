package app.krista.extensions.essentials.cloudfiles.ebonedrive.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OneDriveFiltersTest {

    @Test
    void matchesSearch_blankOrNullSearchMatchesEverything() {
        assertTrue(OneDriveFilters.matchesSearch("report.pdf", null));
        assertTrue(OneDriveFilters.matchesSearch("report.pdf", ""));
        assertTrue(OneDriveFilters.matchesSearch("report.pdf", "   "));
    }

    @Test
    void matchesSearch_caseInsensitiveSubstring() {
        assertTrue(OneDriveFilters.matchesSearch("Quarterly Report.pdf", "report"));
        assertTrue(OneDriveFilters.matchesSearch("Quarterly Report.pdf", "QUARTERLY"));
        assertFalse(OneDriveFilters.matchesSearch("Quarterly Report.pdf", "invoice"));
    }

    @Test
    void matchesSearch_nullItemNameNeverMatchesANonBlankSearch() {
        assertFalse(OneDriveFilters.matchesSearch(null, "report"));
    }

    @Test
    void isOnOrAfter_nullFromDateMatchesEverything() {
        assertTrue(OneDriveFilters.isOnOrAfter(1000L, null));
        assertTrue(OneDriveFilters.isOnOrAfter(null, null));
    }

    @Test
    void isOnOrAfter_comparesEpochMillis() {
        assertTrue(OneDriveFilters.isOnOrAfter(2000L, 1000L));
        assertTrue(OneDriveFilters.isOnOrAfter(1000L, 1000L));
        assertFalse(OneDriveFilters.isOnOrAfter(500L, 1000L));
    }

    @Test
    void isOnOrAfter_nullItemDateWithRealFromDateIsExcluded() {
        assertFalse(OneDriveFilters.isOnOrAfter(null, 1000L));
    }

    @Test
    void joinPath_handlesRootAndTrailingSlash() {
        assertEquals("/Reports", OneDriveFilters.joinPath("", "Reports"));
        assertEquals("/Reports", OneDriveFilters.joinPath("/", "Reports"));
        assertEquals("/Reports/2024", OneDriveFilters.joinPath("/Reports", "2024"));
        assertEquals("/Reports/2024", OneDriveFilters.joinPath("/Reports/", "2024"));
    }

    @Test
    void baseNameAndExtension_splitOnLastDot() {
        assertEquals("report", OneDriveFilters.baseName("report.pdf"));
        assertEquals("pdf", OneDriveFilters.extension("report.pdf"));
        assertEquals("archive.tar", OneDriveFilters.baseName("archive.tar.gz"));
        assertEquals("gz", OneDriveFilters.extension("archive.tar.gz"));
    }

    @Test
    void baseNameAndExtension_noExtension() {
        assertEquals("README", OneDriveFilters.baseName("README"));
        assertEquals("", OneDriveFilters.extension("README"));
    }

    @Test
    void baseNameAndExtension_nullInputReturnsNull() {
        assertNull(OneDriveFilters.baseName(null));
        assertNull(OneDriveFilters.extension(null));
    }

    @Test
    void isRoot_blankOrSlashOrNull() {
        assertTrue(OneDriveFilters.isRoot(null));
        assertTrue(OneDriveFilters.isRoot(""));
        assertTrue(OneDriveFilters.isRoot("   "));
        assertTrue(OneDriveFilters.isRoot("/"));
        assertFalse(OneDriveFilters.isRoot("/Reports"));
    }

    @Test
    void encodePathSegments_encodesEachSegmentButKeepsSlashes() {
        assertEquals("Reports%202024/Q1", OneDriveFilters.encodePathSegments("/Reports 2024/Q1"));
        assertEquals("Reports", OneDriveFilters.encodePathSegments("Reports"));
    }
}

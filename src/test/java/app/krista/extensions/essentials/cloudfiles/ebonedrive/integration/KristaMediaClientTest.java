package app.krista.extensions.essentials.cloudfiles.ebonedrive.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KristaMediaClientTest {

    @Test
    void safeName_replacesIllegalCharactersOnly() {
        assertEquals("report_2024.pdf", KristaMediaClient.safeName("report 2024.pdf".replace(' ', '_')));
        assertEquals("a_b_c.txt", KristaMediaClient.safeName("a/b:c.txt"));
    }

    @Test
    void safeName_leavesSafeNamesUnchanged() {
        assertEquals("Report-Final_v2.pdf", KristaMediaClient.safeName("Report-Final_v2.pdf"));
    }

    @Test
    void safeName_nullBecomesDefaultFileName() {
        assertEquals("file", KristaMediaClient.safeName(null));
    }
}

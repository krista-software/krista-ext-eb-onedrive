package app.krista.extensions.essentials.cloudfiles.ebonedrive.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GraphErrorTranslatorTest {

    @Test
    void invalidClientId_mapsToActionableMessage() {
        String msg = GraphErrorTranslator.toFriendlyMessage(new RuntimeException(
                "AADSTS700016: Application with identifier 'xyz' was not found"));
        assertTrue(msg.contains("Invalid Client ID"));
    }

    @Test
    void invalidClientSecret_mapsToActionableMessage() {
        String msg = GraphErrorTranslator.toFriendlyMessage(new RuntimeException("Invalid client secret provided"));
        assertTrue(msg.contains("Invalid Client Secret"));
    }

    @Test
    void consentRequired_mapsToActionableMessage() {
        String msg = GraphErrorTranslator.toFriendlyMessage(new RuntimeException("AADSTS65001: consent required"));
        assertTrue(msg.contains("Admin consent"));
    }

    @Test
    void forbidden_mapsToPermissionMessage() {
        String msg = GraphErrorTranslator.toFriendlyMessage(new RuntimeException("403 Forbidden"));
        assertTrue(msg.toLowerCase().contains("access denied"));
    }

    @Test
    void notFound_mapsToOneDriveSpecificMessage() {
        String msg = GraphErrorTranslator.toFriendlyMessage(new RuntimeException("itemNotFound: 404"));
        assertTrue(msg.toLowerCase().contains("could not be found"));
    }

    @Test
    void unknownError_fallsBackToGenericMessage() {
        String msg = GraphErrorTranslator.toFriendlyMessage(new RuntimeException("some completely unmapped failure"));
        assertTrue(msg.contains("Unable to connect to Microsoft OneDrive"));
    }

    @Test
    void nullMessage_usesExceptionClassNameAndStillReturnsSomething() {
        String msg = GraphErrorTranslator.toFriendlyMessage(new RuntimeException((String) null));
        assertNotNull(msg);
        assertFalse(msg.isBlank());
    }
}

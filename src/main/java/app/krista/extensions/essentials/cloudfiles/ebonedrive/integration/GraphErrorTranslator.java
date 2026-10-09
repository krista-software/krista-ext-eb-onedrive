package app.krista.extensions.essentials.cloudfiles.ebonedrive.integration;

/**
 * Translates a Microsoft Graph / MSAL exception into a message a non-technical admin can act on. See
 * Trap 0.14 in adding-krista-msauth-tabs/SKILL.md. Never surface a raw SDK/HTTP exception message
 * anywhere a user reads it — run every caught exception through this first, in testConnection() AND
 * every real business-logic catalog request's generic catch block.
 */
public final class GraphErrorTranslator {

    private GraphErrorTranslator() {
    }

    public static String toFriendlyMessage(Throwable e) {
        String msg = e.getMessage();
        if (msg == null) {
            msg = e.getClass().getSimpleName();
        }
        String lower = msg.toLowerCase();

        if (msg.contains("AADSTS700016") || msg.contains("Application with identifier")) {
            return "Invalid Client ID — the application wasn't found in Azure AD. Please verify your Client ID.";
        }
        if (msg.contains("AADSTS7000215") || lower.contains("invalid client secret")) {
            return "Invalid Client Secret — make sure you're using the secret's Value (not its Secret ID) "
                    + "from the Azure Portal.";
        }
        if (msg.contains("AADSTS90002") || (msg.contains("Tenant") && lower.contains("not found"))) {
            return "Tenant ID not found — please verify your Tenant ID.";
        }
        if (msg.contains("AADSTS65001") || lower.contains("consent")) {
            return "Admin consent is required for this application in your Microsoft tenant. Please "
                    + "contact your Microsoft administrator.";
        }
        if (lower.contains("only valid with delegated authentication")) {
            // Should be unreachable — every call site uses .users(email) instead of .me() (Trap 0.14).
            return "This operation isn't supported for the selected sign-in mode. Please contact support "
                    + "— this indicates a configuration issue with this extension.";
        }
        if (lower.contains("unauthorized") || msg.contains(" 401")) {
            return "Authentication failed — the credentials may be invalid or expired. Please re-authorize.";
        }
        if (lower.contains("forbidden") || msg.contains(" 403")) {
            return "Access denied — the application doesn't have permission to perform this action on "
                    + "OneDrive. Please check the configured API permissions (Files.Read.All) in Azure AD.";
        }
        if (lower.contains("itemnotfound") || msg.contains(" 404")) {
            return "The requested file or folder could not be found in OneDrive. Please verify the path, "
                    + "folder ID, or file ID.";
        }
        return "Unable to connect to Microsoft OneDrive. Please verify your credentials and try again.";
    }
}

package app.krista.extensions.essentials.cloudfiles.ebonedrive;

import app.krista.sdk.msauth.config.AuthFlow;
import app.krista.sdk.msauth.extension.annotations.AuditConfig;
import app.krista.sdk.msauth.extension.annotations.AuthScope;
import app.krista.sdk.msauth.extension.annotations.MsAuthExtension;

/**
 * Microsoft-auth-SDK declarative config (Pattern D). Intentionally empty — all auth, scope, and audit
 * configuration is expressed as annotations; the SDK ships the Authentication/Audit tab HTML.
 *
 * supportedFlows covers both mechanisms confirmed for this build:
 *  - AUTH_CODE_PUBLIC: a specific Microsoft user signs in interactively (delegated).
 *  - CLIENT_CREDENTIALS_SECRET: server-to-server, app-only (authType "Private" per Trap 2 — NOT
 *    "customer's own app"; it's always client-credentials).
 */
@MsAuthExtension(
        extensionType = "ebonedrive",
        callbackPathPrefix = "/ebonedrive",
        supportedFlows = {AuthFlow.AUTH_CODE_PUBLIC, AuthFlow.CLIENT_CREDENTIALS_SECRET},
        defaultScopes = {"openid", "offline_access", "User.Read", "Files.Read.All"},
        headerTitle = "EB Onedrive Authentication",
        headerSubtitle = "Configure how Krista connects to Microsoft OneDrive",
        showScopePanel = true,
        supportsOwnCredentials = true,
        enableAuditDashboard = true,
        // Stable EXTERNAL URL — a relative link into this extension's own bundled docs 404s in real
        // testing (the Authentication and Documentation tabs are served from different resource paths
        // with no verified relative depth between them). Compile-time annotation constant — can never
        // be computed from routingUrl at runtime. See Trap 0.15.
        setupGuideUrl = "https://learn.microsoft.com/en-us/entra/identity-platform/quickstart-register-app"
)
// This extension reads OneDrive files/folders only — a single scope covers every real catalog request,
// because every one of them calls the same Graph /drive resource family (Test Connection's own
// `.drive().root()` check needs exactly this, matching Trap 0.14's least-privilege rule).
@AuthScope(value = "Files.Read.All",
        description = "Read-only access to files and folders in the connected OneDrive.", required = true,
        catalogRequests = {"Test Connection", "Health Check", "List Files", "List Folders", "Get Result",
                "Retrieve files using folder id", "Get files and File Id from Folder Id",
                "Fetch Files From Folder", "Get Folder Id by path", "Download as File",
                "Download File using the File Id", "Get the Downloaded file"})
@AuditConfig(
        dashboardTitle = "EB Onedrive Audit Dashboard",
        enableCredentialHealth = true,
        enableCharts = true,
        csvExportPrefix = "ebonedrive-audit"
)
public class EbOnedriveAuthConfig {
}

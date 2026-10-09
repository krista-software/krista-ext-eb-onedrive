# Authentication

EB Onedrive authenticates with **Microsoft Entra ID (Azure AD) OAuth 2.0**, via the same Microsoft
sign-in flow Krista uses for other Microsoft-product extensions (Outlook, SharePoint). Two connection
modes are supported — pick the one that matches how your organization wants to grant access:

- **User Authentication** — a specific Microsoft user signs in interactively (delegated access). The
  extension acts as that person.
- **Server to Server Authentication** — an Azure AD application identity authenticates with a client
  secret, with no end-user sign-in (client-credentials / app-only).

## Quick Setup vs. Your Own Credentials

The Authentication tab normally offers a **Quick Setup** option using a Krista-managed, pre-registered
Azure AD application. **Quick Setup is not available for this deployment** — Krista has not registered
a shared multi-tenant Azure AD app for it yet. Use **Your Own Credentials** (Advanced) instead:

1. In the [Azure Portal](https://portal.azure.com), register a new application under **Microsoft Entra
   ID → App registrations → New registration**.
2. Under **API permissions**, add the Microsoft Graph permission(s) listed in the table below —
   **Delegated** for User Authentication, **Application** for Server to Server Authentication — and
   grant admin consent.
3. Under **Certificates & secrets**, create a new client secret and copy its **Value** (not its Secret
   ID).
4. On the Authentication tab, choose **Your Own Credentials**, select the matching mode, and enter the
   Client ID, Client Secret, and Tenant ID from the app registration. For User Authentication, also
   enter the email address of the account to connect.
5. Click **Test Connection**. A Microsoft sign-in popup appears for User Authentication; Server to
   Server Authentication validates immediately.
6. Click **Save**.

## Required scopes / permissions

| Mode | Required Graph permission | Permission type | Admin consent required? |
|---|---|---|---|
| **User Authentication** (delegated — a specific user signs in) | Files.Read.All | Delegated | Only if your tenant restricts user consent |
| **Server to Server Authentication** (client credentials — no user sign-in) | Files.Read.All | **Application** | Always yes |

Grant only the permission type matching the mode you actually use — a Delegated grant does nothing for
Server to Server (client-credentials) mode, and vice versa; Azure AD evaluates these as two completely
separate permission sets. `Files.Read.All` is the only permission this extension needs — every request
(listing and downloading) reads from the `/drive` resource and nothing else, and Test Connection
exercises that exact same resource (`drive().root()`), so a successful Test Connection confirms the
real feature will work too.

## Verifying

Run **Test Connection** on the Authentication tab (checks the form you just filled in) or on the plain
Setup tab (checks what's already saved). A successful result confirms the credentials, consent, and
`Files.Read.All` permission are all in place; a failure returns an actionable message (e.g. invalid
client secret, consent required, access denied).

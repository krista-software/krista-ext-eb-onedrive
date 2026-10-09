# Release Notes

## Version 1.0.0

- **Developer Name** : Krista Extension Builder
- **Krista Service APIs Java** : 1.0.125-sp1
- **Global Catalog Version** : Not Available

Initial release — list files, list folders, and download files from Microsoft OneDrive.

### New Features

* List the files and folders inside a OneDrive folder (or the drive root), with an optional search
  filter, via the Task-ID/Get-Result pattern shared with the rest of the Cloud Files domain.
* List files inside a folder identified by either its OneDrive id or its path, optionally filtered by
  modification date.
* Resolve a folder's path to its OneDrive id.
* Download a file's content, identified by either its path or its OneDrive id, as a Krista file.
* Microsoft Entra ID (Azure AD) authentication supporting both interactive user sign-in and
  server-to-server (client-credentials) connections.

### Resolved Bugs

* Not Available

### Dependency Updates

* Not Available

### Known Issues

* [Get the Downloaded file](pages/GetTheDownloadedFile.md) has no producer in this release — see its
  page for details. It's fully implemented and ready for a future background-download request.

### Limitations and Caveats

* One OneDrive account per connection (invoker) — no per-end-user sign-in.
* Requires the `Files.Read.All` Microsoft Graph permission (Delegated or Application, matching the
  connection mode) — see [Authentication](pages/Authentication.md).
* Quick Setup is not available for this deployment; use Your Own Credentials.
* Listing results are capped at 25 Graph pages (up to 5,000 items) per call as a safety limit for very
  large folders.

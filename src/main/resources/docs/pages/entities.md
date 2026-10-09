# Entities

Data structures returned by the requests. Request output tables link here instead of repeating the
full field list. All three entities are reused verbatim (same name, id, and field shape) from the
existing Essentials/Cloud Files domain catalog.

## Custom File

The generic, mixed file-or-folder item returned by [Get Result](pages/GetResult.md) — the asynchronous
result of [List Files](pages/ListFiles.md) and [List Folders](pages/ListFolders.md). The **Is Folder**
field is what lets one shared list represent both files and folders.

| Field | Type | Description |
|---|---|---|
| Name | Text | The item's display name. |
| Base Name | Text | The name without its file extension. |
| Extension | Text | The file extension, without the dot. Empty for folders. |
| File Id | Text | The OneDrive drive-item id. Primary key. |
| Created | Date | When the item was created in OneDrive. |
| Size | Number | Size in bytes (0 for folders). |
| Absolute Path | Text | The item's full path within the drive. |
| Parent Folder | Text | The path of the containing folder. |
| Is Folder | Switch | True when this item is a folder, false when it is a file. |

## File

A single OneDrive file, returned by [Retrieve files using folder id](pages/RetrieveFilesUsingFolderId.md),
[Get files and File Id from Folder Id](pages/GetFilesAndFileIdFromFolderId.md), and
[Fetch Files From Folder](pages/FetchFilesFromFolder.md).

| Field | Type | Description |
|---|---|---|
| Name | Text | The file's display name. |
| Base Name | Text | The name without its file extension. |
| Extension | Text | The file extension, without the dot. |
| Parent Folder | Text | The path of the containing folder. |
| Absolute Path | Text | The file's full path within the drive. Primary key. |
| Owner | Text | Display name of the file's creator, when known. |
| Created | Date | When the file was created. |
| Modified | Date | When the file was last modified. |
| Size | Number | Size in bytes. |
| Permissions | Text | Not populated — would require a separate Graph permissions call. |
| Is Folder | Switch | Always false for a File entity. |
| Tags | [ Text ] | Not populated — OneDrive drive items don't carry a tags concept. |
| Shared With | [ Text ] | Not populated by the drive-children listing. |
| Created By | Text | Display name of who created the file. |
| Modified By | Text | Display name of who last modified the file. |
| File | File | The downloaded file content — only populated by the download requests, not the listing requests. |

## Folder

Reused and registered for this domain, but no request in this build produces a standalone Folder
entity today — List Folders' results come back through the shared **Custom File** shape above instead.
It's documented here for completeness and so a future folder-detail request (in this or another Cloud
Files extension) can reuse it.

| Field | Type | Description |
|---|---|---|
| Name | Text | |
| Base Name | Text | |
| Extension | Text | |
| Parent Folder | Text | |
| Absolute Path | Text | |
| Owner | Text | |
| Created | Date | |
| Modified | Date | |
| Size | Number | |
| Permissions | Text | |
| Is Folder | Switch | |
| Folder ID | Text | Primary key. |
| Tags | [ Text ] | |
| Created By | Text | |
| Modified By | Text | |
| File | File | |
| Shared With | [ Text ] | |

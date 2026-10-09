# Supported Requests

EB Onedrive exposes **12 catalog requests** across **2 areas**. Each links to its detailed page.

## Setup
| Request | Type | Description |
|---|---|---|
| [Test Connection](pages/TestConnection.md) | QUERY_SYSTEM | Validates connectivity and authentication against OneDrive using the saved configuration. |
| [Health Check](pages/HealthCheck.md) | CHANGE_SYSTEM | Reports JVM health plus a live OneDrive connectivity check. |

## Management
| Request | Type | Description |
|---|---|---|
| [List Files](pages/ListFiles.md) | QUERY_SYSTEM | Lists the files in a folder; returns a Task ID for [Get Result](pages/GetResult.md). |
| [List Folders](pages/ListFolders.md) | QUERY_SYSTEM | Lists the folders in a folder; returns a Task ID for [Get Result](pages/GetResult.md). |
| [Get Result](pages/GetResult.md) | WAIT_FOR_EVENT | Returns the items found by List Files/List Folders for a given Task ID. |
| [Retrieve files using folder id](pages/RetrieveFilesUsingFolderId.md) | CHANGE_SYSTEM | Lists files directly inside a folder identified by its id. |
| [Get files and File Id from Folder Id](pages/GetFilesAndFileIdFromFolderId.md) | CHANGE_SYSTEM | Lists files directly inside a folder identified by its id. |
| [Fetch Files From Folder](pages/FetchFilesFromFolder.md) | CHANGE_SYSTEM | Lists files directly inside a folder identified by its path. |
| [Get Folder Id by path](pages/GetFolderIdByPath.md) | CHANGE_SYSTEM | Resolves a folder's path to its OneDrive drive-item id. |
| [Download as File](pages/DownloadAsFile.md) | CHANGE_SYSTEM | Downloads a file's content, identified by its path. |
| [Download File using the File Id](pages/DownloadFileUsingTheFileId.md) | CHANGE_SYSTEM | Downloads a file's content, identified by its drive-item id. |
| [Get the Downloaded file](pages/GetTheDownloadedFile.md) | WAIT_FOR_EVENT | Returns a background download job's file (no producer in this build — see its page). |

# Download File using the File Id

## Overview
Downloads a file's content, identified by its OneDrive drive-item id (rather than its path, as
[Download as File](pages/DownloadAsFile.md) requires), and returns it as a Krista file.

## Request Details
- **Area**: Management
- **Type**: CHANGE_SYSTEM
- **External API**: `GET https://graph.microsoft.com/v1.0/users/{email}/drive/items/{fileId}/content`
## Input Parameters
| Parameter | Type | Required | Description |
|---|---|---|---|
| File Id | Text | Yes | The file's OneDrive drive-item id (e.g. from [Get files and File Id from Folder Id](pages/GetFilesAndFileIdFromFolderId.md)). |

## Output Parameters
| Parameter | Type | Description |
|---|---|---|
| File | File | The downloaded file content. |

## Example
**Input**: `File Id="01DEF456..."`
**Output**: `File=<file reference>`

## Error Handling
- **File Id is required** — a blank File Id is rejected.
- **File Id not found** — surfaces a friendly "could not be found" message.

# Get Folder Id by path

## Overview
Resolves a folder's path to its OneDrive drive-item id — useful for feeding into
[Retrieve files using folder id](pages/RetrieveFilesUsingFolderId.md) or
[Get files and File Id from Folder Id](pages/GetFilesAndFileIdFromFolderId.md).

## Request Details
- **Area**: Management
- **Type**: CHANGE_SYSTEM
- **External API**: `GET https://graph.microsoft.com/v1.0/users/{email}/drive/root:/{path}`
## Input Parameters
| Parameter | Type | Required | Description |
|---|---|---|---|
| Path | Text | Yes | The folder's path, e.g. `/Reports/2024`. |

## Output Parameters
| Parameter | Type | Description |
|---|---|---|
| Folder Id | Text | The folder's OneDrive drive-item id. |

## Example
**Input**: `Path="/Reports/2024"`
**Output**: `Folder Id="01ABCXYZ..."`

## Error Handling
- **Path is required** — a blank Path is rejected.
- **Path not found** — surfaces a friendly "could not be found" message.

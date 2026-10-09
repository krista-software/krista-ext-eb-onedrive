# Get files and File Id from Folder Id

## Overview
Returns the files directly inside a folder identified by its OneDrive folder id. Functionally
equivalent to [Retrieve files using folder id](pages/RetrieveFilesUsingFolderId.md) — both exist
because they're reused verbatim from the existing Cloud Files domain catalog, which already published
them as two separate requests.

## Request Details
- **Area**: Management
- **Type**: CHANGE_SYSTEM
- **External API**: `GET https://graph.microsoft.com/v1.0/users/{email}/drive/items/{folderId}/children`
## Input Parameters
| Parameter | Type | Required | Description |
|---|---|---|---|
| Folder Id | Text | Yes | The OneDrive drive-item id of the folder to list. |
| From Date | Date | No | Only include files modified on or after this date. |

## Output Parameters
| Parameter | Type | Description |
|---|---|---|
| List of Files | [ Entity(File) ] | The files found. See [Entities](pages/entities.md). |

## Example
**Input**: `Folder Id="01ABCXYZ..."`
**Output**: `List of Files=[ {Name: "q1.xlsx", ...}, {Name: "q2.xlsx", ...} ]`

## Error Handling
- **Folder Id is required** — a blank Folder Id is rejected.
- **Folder not found** — surfaces a friendly "could not be found" message.

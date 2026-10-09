# Retrieve files using folder id

## Overview
Returns the files (not subfolders) directly inside a folder identified by its OneDrive folder id,
synchronously — no Task ID round-trip needed.

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
| Response | [ Entity(File) ] | The files found. See [Entities](pages/entities.md). |

## Example
**Input**: `Folder Id="01ABCXYZ..."`, `From Date=2024-01-01`
**Output**: `Response=[ {Name: "q1.xlsx", Modified: "2024-02-10", ...} ]`

## Error Handling
- **Folder Id is required** — a blank Folder Id is rejected.
- **Folder not found** — surfaces a friendly "could not be found" message.

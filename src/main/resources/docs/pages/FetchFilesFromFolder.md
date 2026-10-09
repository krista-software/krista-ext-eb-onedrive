# Fetch Files From Folder

## Overview
Returns the files directly inside a folder identified by its **path** (rather than its id, as
[Retrieve files using folder id](pages/RetrieveFilesUsingFolderId.md) requires).

## Request Details
- **Area**: Management
- **Type**: CHANGE_SYSTEM
- **External API**: `GET https://graph.microsoft.com/v1.0/users/{email}/drive/root:/{path}:/children`
## Input Parameters
| Parameter | Type | Required | Description |
|---|---|---|---|
| Path | Text | Yes | The folder to list, e.g. `/Reports`. Use `/` for the drive root. |
| From Date | Date | No | Only include files modified on or after this date. |

## Output Parameters
| Parameter | Type | Description |
|---|---|---|
| Response | [ Entity(File) ] | The files found. See [Entities](pages/entities.md). |

## Example
**Input**: `Path="/Reports"`
**Output**: `Response=[ {Name: "q1.xlsx", ...} ]`

## Error Handling
- **Path is required** — a blank Path is rejected; pass `/` explicitly for the root.
- **Path not found** — surfaces a friendly "could not be found" message.

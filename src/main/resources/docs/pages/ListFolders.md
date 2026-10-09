# List Folders

## Overview
Produces a non-recursive list of folders present within the folder denoted by the source path. Returns
a **Task ID** immediately — call [Get Result](pages/GetResult.md) with that Task ID to retrieve the
actual list of folders.

## Request Details
- **Area**: Management
- **Type**: QUERY_SYSTEM
- **External API**: `GET https://graph.microsoft.com/v1.0/users/{email}/drive/root:/{path}:/children`
## Input Parameters
| Parameter | Type | Required | Description |
|---|---|---|---|
| Source Path | Text | Yes | The folder to list, e.g. `/Reports`. Use `/` for the drive root. |
| Search | Text | No | Case-insensitive substring filter on folder name. Blank matches every folder. |

## Output Parameters
| Parameter | Type | Description |
|---|---|---|
| Task ID | Text | Pass this to [Get Result](pages/GetResult.md) to retrieve the folders found. |

## Example
**Input**: `Source Path="/"`, `Search="2024"`
**Output**: `Task ID="task_8b1d4a22-..."`

## Error Handling
- **Source Path is required** — a blank Source Path is rejected; pass `/` explicitly for the root.
- **Path not found** — surfaces a friendly "could not be found" message if the folder doesn't exist.

## Technical Implementation
Shares its async Task-ID/Get-Result mechanism with [List Files](pages/ListFiles.md) — see that page's
Technical Implementation note for the full reasoning.

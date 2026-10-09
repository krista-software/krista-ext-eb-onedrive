# List Files

## Overview
Produces a non-recursive list of files present within the folder denoted by the source path. Returns
a **Task ID** immediately — call [Get Result](pages/GetResult.md) with that Task ID to retrieve the
actual list of files.

## Request Details
- **Area**: Management
- **Type**: QUERY_SYSTEM
- **External API**: `GET https://graph.microsoft.com/v1.0/users/{email}/drive/root:/{path}:/children`
## Input Parameters
| Parameter | Type | Required | Description |
|---|---|---|---|
| Source Path | Text | Yes | The folder to list, e.g. `/Reports/2024`. Use `/` for the drive root. |
| Search | Text | No | Case-insensitive substring filter on file name. Blank matches every file. |

## Output Parameters
| Parameter | Type | Description |
|---|---|---|
| Task ID | Text | Pass this to [Get Result](pages/GetResult.md) to retrieve the files found. |

## Example
**Input**: `Source Path="/Reports"`, `Search=""`
**Output**: `Task ID="task_3f9c2e10-..."`

## Error Handling
- **Source Path is required** — a blank Source Path is rejected; pass `/` explicitly for the root.
- **Path not found** — surfaces a friendly "could not be found" message if the folder doesn't exist.

## Technical Implementation
Microsoft Graph's children listing is natively synchronous, but this request still returns a Task ID
to match this domain's existing, already-published List Files contract exactly (see
[Get Result](pages/GetResult.md) for the full mechanics). The full result set is fetched and cached
internally (following every Graph page, up to a safety cap) before the Task ID is returned, so Get
Result always has the complete list ready on its very first call.

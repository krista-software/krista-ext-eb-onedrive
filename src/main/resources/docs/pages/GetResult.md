# Get Result

## Overview
Accepts a Task ID produced by [List Files](pages/ListFiles.md) or [List Folders](pages/ListFolders.md)
and returns the items found, each as a **Custom File** entity whose `Is Folder` field tells you whether
it's a file or a folder.

## Request Details
- **Area**: Management
- **Type**: WAIT_FOR_EVENT
- **External API**: none — reads a result already fetched by List Files/List Folders.
## Input Parameters
| Parameter | Type | Required | Description |
|---|---|---|---|
| Task ID | Text | Yes | The Task ID returned by List Files or List Folders. |

## Output Parameters
| Parameter | Type | Description |
|---|---|---|
| Files | [ Entity(Custom File) ] | The files or folders found. See [Entities](pages/entities.md). |
| Success | Boolean | False if the Task ID is unknown or has expired. |
| Message | Text | A human-readable summary (e.g. "Found 12 files."). |

## Example
**Input**: `Task ID="task_3f9c2e10-..."`
**Output**: `Files=[ {Name: "report.pdf", Is Folder: false, ...}, ... ]`, `Success=true`,
`Message="Found 3 files."`

## Error Handling
- **Unknown or expired Task ID** — returns `Success=false` with an explanatory message rather than an
  error, since the Task ID itself was syntactically valid input.

## Technical Implementation
List Files/List Folders fetch the complete result eagerly and store it keyed by Task ID, then fire an
internal platform event named after that Task ID. This request is the event's consumer: it matches on
the event name (not an external webhook — the event is generated internally, by this same extension)
and returns the stored result. This two-step shape matches the already-published Cloud Files domain
contract for this request exactly.

# Download as File

## Overview
Downloads a file's content, identified by its path, and returns it as a Krista file your workflow can
attach, read, or forward.

## Request Details
- **Area**: Management
- **Type**: CHANGE_SYSTEM
- **External API**: `GET https://graph.microsoft.com/v1.0/users/{email}/drive/root:/{path}:/content`
## Input Parameters
| Parameter | Type | Required | Description |
|---|---|---|---|
| Source | Text | Yes | The file's path, e.g. `/Reports/q1.xlsx`. |

## Output Parameters
| Parameter | Type | Description |
|---|---|---|
| Krista File | File | The downloaded file content. |
| Success | Boolean | True when the download succeeded. |
| Message | RichText | A human-readable summary. |

## Example
**Input**: `Source="/Reports/q1.xlsx"`
**Output**: `Krista File=<file reference>`, `Success=true`, `Message="Downloaded successfully."`

## Error Handling
- **Source is required** — a blank Source is rejected.
- **Source not found, or is a folder** — surfaces a friendly "could not be found" message.

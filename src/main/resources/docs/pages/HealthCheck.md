# Health Check

## Overview
Reports the extension's JVM health alongside a live OneDrive connectivity check.

## Request Details
- **Area**: Setup
- **Type**: CHANGE_SYSTEM
- **External API**: `GET https://graph.microsoft.com/v1.0/users/{email}/drive/root`
## Input Parameters
None.

## Output Parameters
| Parameter | Type | Description |
|---|---|---|
| Is Healthy | Boolean | True when both the JVM and the OneDrive connection are healthy. |
| Heap Used MB | Number | Current JVM heap usage in megabytes. |
| Details | Text | A human-readable summary. |

## Example
**Input**: (none)
**Output**: `Is Healthy=true`, `Heap Used MB=128`, `Details="OK — connected to OneDrive as jane@contoso.com."`

## Error Handling
- If the OneDrive connectivity check fails, `Is Healthy=false` and `Details` carries a friendly,
  actionable error message — the request itself still returns success (this is a status report, not a
  failing call).

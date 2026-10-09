# Test Connection

## Overview
Validates connectivity and authentication against Microsoft OneDrive using the already-saved
configuration. Use this to confirm the connection is healthy without opening the Authentication tab.

## Request Details
- **Area**: Setup
- **Type**: QUERY_SYSTEM
- **External API**: `GET https://graph.microsoft.com/v1.0/users/{email}/drive/root`
## Input Parameters
None.

## Output Parameters
| Parameter | Type | Description |
|---|---|---|
| Success | Boolean | True when OneDrive responded successfully. |
| Message | Text | A human-readable confirmation or error message. |

## Example
**Input**: (none)
**Output**: `Success=true`, `Message="Connected to OneDrive as jane@contoso.com."`

## Error Handling
- **Not configured** — if the Authentication tab hasn't been completed yet, the request fails with a
  message pointing you there.
- **Invalid/expired credentials** — returns a friendly, actionable message (e.g. "Admin consent is
  required...", "Invalid Client Secret...") rather than a raw Microsoft Graph error.

# Get the Downloaded file

## Overview
Accepts a Task Id for a backgrounded download job and returns the downloaded file once it completes.

> **Note:** this request is reused verbatim from the existing Cloud Files domain catalog, and is fully,
> correctly implemented as an event consumer — but no request in this build currently produces a
> background download job for it to consume. [Download as File](pages/DownloadAsFile.md) and
> [Download File using the File Id](pages/DownloadFileUsingTheFileId.md) both return their file
> synchronously instead, matching their own declared (non-Task-ID) output shape. Calling this request
> today will always report an unknown Task Id. It's included and kept correct so a future background
> ("download as a job") request in this domain can populate it without any change here.

## Request Details
- **Area**: Management
- **Type**: WAIT_FOR_EVENT
- **External API**: none — reads a result from a background download job, when one exists.
## Input Parameters
| Parameter | Type | Required | Description |
|---|---|---|---|
| Task Id | Text | Yes | The Task Id of a background download job. |

## Output Parameters
| Parameter | Type | Description |
|---|---|---|
| File | File | The downloaded file content, once the job completes. |
| Success | Boolean | False if the Task Id is unknown or has expired. |
| Message | Text | A human-readable summary. |

## Example
**Input**: `Task Id="task_unknown"`
**Output**: `File=null`, `Success=false`, `Message="Unknown or expired Task Id: task_unknown"`

## Error Handling
- **Unknown or expired Task Id** — returns `Success=false` with an explanatory message (this is the
  only outcome possible today — see the note above).

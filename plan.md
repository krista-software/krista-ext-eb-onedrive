# EB Onedrive — Implementation Plan

## Summary
A Krista Global Catalog extension integrating **Microsoft OneDrive** (via the Microsoft Graph API,
`https://graph.microsoft.com/v1.0`) into the **Essentials / Cloud Files** domain. It delivers the
three requested capabilities — listing files, listing folders, and downloading file content — but,
per the domain-reuse review, conforms to the **existing, already-published Cloud Files request/entity
contracts** rather than inventing OneDrive-specific names, so this extension's catalog surface reads
identically to the other providers already registered in Cloud Files (no naming drift across vendors).

- **Extension name (slug):** `eb-onedrive`
- **Title:** EB Onedrive
- **Version:** 1.0.0
- **Package:** `app.krista.extensions.essentials.cloudfiles.ebonedrive`
- **Ecosystem / Domain:** Essentials / Cloud Files (existing domain — see Domain reuse below)

## Authentication — Pattern D (Microsoft)
OneDrive is a Microsoft product, so Krista's non-negotiable rule applies: **Pattern D** regardless of
the wizard's `oauth2-client-credentials,oauth2-interactive` answer. Pattern D's `msauth` building
blocks natively support both of those as connection modes:
- **Public / delegated (interactive sign-in)** — calls Graph as the signed-in user (`/me/drive`).
- **Private / client-credentials (app-only, server-to-server)** — calls Graph as the application
  against a specific user's drive (`/users/{id}/drive`), per the `adding-krista-msauth-tabs` skill's
  established dispatch pattern (never a bare `/me` call under app-only auth).

Both modes will be offered as admin-configurable connection options on the Setup tab, consistent with
how other Microsoft-product extensions (Outlook, SharePoint) in this codebase already do it.

## Area: Management

| Request | Type | Inputs | Output | External endpoint | Reused / New |
|---|---|---|---|---|---|
| List Files | QUERY_SYSTEM | Source Path (Text, required), Search (Text, optional) | Task ID (Text) | `GET /me/drive/items/{itemId}/children` (itemId resolved from Source Path); queued as an async job | **Reused** — `localDomainRequest_f5f0a870-…` |
| List Folders | QUERY_SYSTEM | Source Path (Text, required), Search (Text, optional) | Task ID (Text) | Same children endpoint, folder-only subset; shares the Task-ID/Get Result pair | **Reused** — `localDomainRequest_3f16a853-…` |
| Get Result | WAIT_FOR_EVENT | Task ID (Text, required) | `[ Entity(Custom File) ]`, Success (Switch), Message (Text) | Internal job-result lookup — maps queued Graph children to Custom File (`Is Folder` distinguishes files/folders); surfaces a continuation reference when more Graph pages remain | **Reused** — `localDomainRequest_39c4d468-…` |
| Retrieve files using folder id | CHANGE_SYSTEM | Folder Id (Text, required), From Date (Date, optional) | `[ Entity(File) ]` | `GET /me/drive/items/{Folder Id}/children`, filtered to files, by `lastModifiedDateTime` | **Reused** — `localDomainRequest_fcefb078-…` |
| Get files and File Id from Folder Id | CHANGE_SYSTEM | Folder Id (Text, required), From Date (Date, optional) | `[ Entity(File) ]` | Same children endpoint, file-only | **Reused** — `localDomainRequest_a5e85205-…` |
| Fetch Files From Folder | CHANGE_SYSTEM | Path (Text, required), From Date (Date, optional) | `[ Entity(File) ]` | `GET /me/drive/root:/{Path}:/children`, filtered to files | **Reused** — `localDomainRequest_7f15df55-…` |
| Get Folder Id by path | CHANGE_SYSTEM | Path (Text, required) | Folder Id (Text) | `GET /me/drive/root:/{Path}` | **Reused** — `localDomainRequest_3513c211-…` |
| Download as File | CHANGE_SYSTEM | Source (Text, required) | Entity(File), Success (Switch), Message (RichText) | `GET /me/drive/root:/{Source}:/content` | **Reused** — `localDomainRequest_d0aea3ac-…` |
| Download File using the File Id | CHANGE_SYSTEM | File Id (Text, required) | Entity(File) | `GET /me/drive/items/{File Id}/content` | **Reused** — `localDomainRequest_4e95bf64-…` |
| Get the Downloaded file | WAIT_FOR_EVENT | Task Id (Text, required) | Entity(File), Success (Switch), Message (Text) | Internal job-result lookup for a backgrounded download job | **Reused** — `localDomainRequest_a151c209-…` |

**Note on the async Task-ID pattern:** Microsoft Graph's list/download calls are natively synchronous,
but the Cloud Files domain's existing contract for `List Files`/`List Folders`/background downloads is
task-ID based (`QUERY_SYSTEM` → Task ID → `WAIT_FOR_EVENT` fetch). To honor the reused contract exactly
as published, this extension will run the Graph call behind an internal lightweight job
(synchronous under the hood for small folders, genuinely asynchronous when a folder is large enough
to need multiple Graph pages), rather than inventing a different, OneDrive-only synchronous shape.

**Entity-mapping assumption (flagged for confirmation during build):** `domain-catalog.json` records
output *types* (`List`, `File`, …) but not which concrete entity backs each one. This plan assumes:
- `Get Result` (the generic async list, backing both List Files and List Folders) → **Custom File**,
  using its `Is Folder` field to distinguish files from folders in one shared result shape.
- The three folder-id/path "list files" variants and the three download requests → **File**.
- **Folder** is reused and registered for this domain but has no direct producer among the ten
  in-scope requests — it's available for an eventual folder-detail request; nothing new was invented
  for it, and no capability was lost by not using it yet.

## Capabilities
| Capability | Enabled | Why |
|---|---|---|
| Entity search | No | Not requested; no searchable-object capability was called for |
| Webhooks / `WAIT_FOR_EVENT` | **Yes** | `Get Result` and `Get the Downloaded file` are `WAIT_FOR_EVENT` requests requiring the event/job-completion infrastructure, even though the "event" is internally generated (job completion), not a push notification from OneDrive itself |
| MCP (`tool = true`) | No | Not requested |
| Solutions tab | No | Not requested |
| File handling (`KristaMediaClient`/`FileRepository`) | **Yes** | Download requests move real file bytes between Graph and Krista |

## Domain reuse
`Essentials / Cloud Files` already had a full, published Cloud Files integration. Per the author's
per-item review:
- **Reused verbatim (exact title/id/field shape, no duplicates created):** entities **Custom File**,
  **Folder**, **File**; requests **List Files**, **List Folders**, **Get Result**, **Retrieve files
  using folder id**, **Get files and File Id from Folder Id**, **Fetch Files From Folder**, **Get
  Folder Id by path**, **Download as File**, **Download File using the File Id**, **Get the Downloaded
  file**.
- **Excluded from scope (avoided):** **Get Response** (`localDomainRequest_940bb32f-…`, Sharepoint
  area) — a duplicate of the reused `Get Result`, dropped per the author's review. It appears nowhere
  else in this build.
- **Genuinely new:** none — everything this build needs already existed in the domain in a compatible
  shape.

## Placeholder IDs — must replace before deploy
- **`@Domain` id / `ecosystemId` / `ecosystemName` / `ecosystemVersion`** for Essentials / Cloud Files:
  `domain-catalog.json` confirms this domain already exists and is published, but does not expose the
  actual registered `@Domain`/ecosystem UUIDs — only entity/request ids within it. This build will mint
  **stable placeholder UUIDs** so the code compiles, but they are **not** the real registered values.
  Before this extension can bind to the real Cloud Files domain, copy the real
  `{{DOMAIN_ID}}`/`{{ECOSYSTEM_ID}}` from a sibling Cloud Files extension (e.g. the one that already
  implements `List Files`/`Download as File` today) — mismatched IDs will silently fail to bind rather
  than erroring loudly.
- Local `localDomainRequest_*` / `localDomainEntity_*` ids for the **reused** items are copied verbatim
  from `domain-catalog.json` (not placeholders — they identify the existing published contract).

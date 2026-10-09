# Overview

EB Onedrive connects Krista to **Microsoft OneDrive** via the Microsoft Graph API. Use it to list the
files and folders in a OneDrive account and download file content directly from Krista workflows and
agents.

This extension's requests are deliberately named and shaped to match the existing
**Essentials / Cloud Files** domain's published catalog — the same business concepts (List Files, List
Folders, Download as File, …) that other Cloud Files providers in this domain already expose — rather
than inventing OneDrive-specific names.

## Use cases

- **Browse a OneDrive folder** — list the files and folders directly inside a folder (or the drive
  root), optionally filtered by a search term.
- **List files within a known folder** — given a folder's id or path, retrieve the files it contains,
  optionally filtered to those modified after a given date.
- **Download a file** — fetch a file's content, identified either by its path or its OneDrive file id,
  as a Krista file your workflow can attach, read, or forward.

## What you can do

The extension exposes **12 requests** across **2 areas** — see
[Supported Requests](pages/supported-requests.md) for the full list, or the sidebar for each request's
detailed page.

## Before you start

1. Obtain credentials — see [Authentication](pages/Authentication.md).
2. Configure the connection on the **Authentication** tab (not the plain Setup tab — OneDrive uses
   Krista's Microsoft sign-in flow).
3. Run **Test Connection** (Setup area, or the Authentication tab's own Test Connection) to verify
   connectivity.

## How listing results come back

**List Files** and **List Folders** return a **Task ID** immediately; call **Get Result** with that
Task ID to retrieve the actual list of items. This two-step shape matches how this request is already
published elsewhere in the Cloud Files domain — see each request's own page for the exact mechanics.

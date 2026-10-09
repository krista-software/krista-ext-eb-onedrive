# Configuration

EB Onedrive uses Krista's Microsoft-product connection model: there are **no plain Setup-tab fields**
to fill in directly. Instead, configuration happens on the dedicated **Authentication** tab, which
walks you through either Quick Setup or registering your own Azure AD application — see
[Authentication](pages/Authentication.md) for the full steps.

## Setup tab

The plain Setup tab still has a **Test Connection** button, which checks whatever was already saved on
the Authentication tab — useful for confirming the connection is healthy without opening the
Authentication tab again. If nothing has been configured yet, it will tell you to open the
Authentication tab first.

## Notes

- Each invoker (connection) connects to exactly **one** OneDrive account/mailbox — this extension does
  not support per-end-user sign-in. All requests act against that one configured account.
- Both **User Authentication** (a specific person signs in) and **Server to Server Authentication**
  (an application identity, no sign-in) are supported — pick whichever matches how your organization
  wants to grant access. See [Authentication](pages/Authentication.md) for the required Microsoft
  Graph permissions for each mode.

---
sidebar_position: 2
---

# Configuring a role
To mark a role as being a role that requires approval through the bot, you need to configure it.
To do so run the `/pim configure` command.

This command takes the following arguments:
1. The role name.
2. Whether the role requires approval.
3. The time in seconds that the role should be assigned, once approved. Use 0 for infinite.
4. The channel in which the approval request thread should be created.
5. The role which should grant approval.

:::warning
There are several aspects that need to be considered when setting up a role for management through the bot:
- Once a role is configured for use through the PIM bot, it can not be removed.
- Due to the ordered nature of Discords role and permission system, it is paramount that the role you configure needs to be above the highest role that any requester has. Else a requester can assign it themselves.
:::
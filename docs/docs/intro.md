---
slug: '/'
sidebar_position: 1
---

# Introduction

Welcome to the documentation for the NeoForged PIM Bot for Discord.
Its primary purpose is to allow moderators to gain access to roles which could be used to harm the general community if abused.

## Usage
### Moderators
Moderators can run the `/pim request` command to request a specific role, while supplying a reason as to why they need the role in the first place.
This will open a thread in a specified channel and add approvers to the thread.
Each approver can then independently approve or reject the request.

### Operators
Operators of the bot can use `/pim configure` to configure a role to be used through the bot.
Configuration options for each role are:
- Whether the role needs approval, or the request is just for auditing purposes.
- How long the approval is granted, after this timeframe runs out, the role assignment is removed.
- In which channel the approval thread should be created.
- Which role can approve the assignment.
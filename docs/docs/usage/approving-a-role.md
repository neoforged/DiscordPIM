---
sidebar_position: 2
---

# Approving a role
Once a community member has created a request for a role, the bot will create a thread, add you to it, and ping you in the process, to draw your attention.
The thread is opened with a message that follows the following structure:

```markdown
## PIM Role: <Rolename> has been requested by: <Username>.
Please validate the request and approve it.

### Reason:
<Reason>
```

Under the message there will be two buttons, one to accept, one to reject.
Pressing either one, will perform the action of said button: One accepts, the other rejects respectively.

You are free to discuss the request, and can if you want even invite the requester to read the thread, by pinging him or her yourself.
However, by default the bot will not add the requester to the thread, even if they were technically allowed to approve the request based on the role's configuration.
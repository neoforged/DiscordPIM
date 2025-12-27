package net.neoforged.discord.bots.pim.dba.model;

public class PendingRoleRequest {
    public long id;
    public long guildId;
    public String role;
    public long userId;
    public long approvalThreadId = -1;
    public String reason;

    public PendingRoleRequest() {}

    // Getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getGuildId() { return guildId; }
    public void setGuildId(long guildId) { this.guildId = guildId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public long getApprovalThreadId() { return approvalThreadId; }
    public void setApprovalThreadId(long approvalThreadId) { this.approvalThreadId = approvalThreadId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

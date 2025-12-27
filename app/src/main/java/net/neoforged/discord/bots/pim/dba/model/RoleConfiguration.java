package net.neoforged.discord.bots.pim.dba.model;

public class RoleConfiguration {
    public long id;
    public String name;
    public long guildId;
    public boolean requiresApproval;
    public int grantedTimeInSeconds;
    public long approvalChannelId;
    public long approvalRoleId;

    public RoleConfiguration() {}

    // Getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getGuildId() { return guildId; }
    public void setGuildId(long guildId) { this.guildId = guildId; }
    public boolean isRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }
    public int getGrantedTimeInSeconds() { return grantedTimeInSeconds; }
    public void setGrantedTimeInSeconds(int grantedTimeInSeconds) { this.grantedTimeInSeconds = grantedTimeInSeconds; }
    public long getApprovalChannelId() { return approvalChannelId; }
    public void setApprovalChannelId(long approvalChannelId) { this.approvalChannelId = approvalChannelId; }
    public long getApprovalRoleId() { return approvalRoleId; }
    public void setApprovalRoleId(long approvalRoleId) { this.approvalRoleId = approvalRoleId; }
}

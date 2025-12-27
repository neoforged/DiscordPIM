package net.neoforged.discord.bots.pim.dba.model;

public class RoleRemovalJob {
    public long id;
    public String roleName;
    public long guildId;
    public long roleId;
    public long userId;
    public int grantedTimeInSeconds;
    public long createdAt;

    public RoleRemovalJob() {}

    // Getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public long getGuildId() { return guildId; }
    public void setGuildId(long guildId) { this.guildId = guildId; }
    public long getRoleId() { return roleId; }
    public void setRoleId(long roleId) { this.roleId = roleId; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public int getGrantedTimeInSeconds() { return grantedTimeInSeconds; }
    public void setGrantedTimeInSeconds(int grantedTimeInSeconds) { this.grantedTimeInSeconds = grantedTimeInSeconds; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}

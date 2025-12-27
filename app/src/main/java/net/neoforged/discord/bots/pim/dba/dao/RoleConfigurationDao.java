package net.neoforged.discord.bots.pim.dba.dao;

import net.neoforged.discord.bots.pim.dba.model.RoleConfiguration;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.*;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import java.util.List;

@RegisterBeanMapper(RoleConfiguration.class)
public interface RoleConfigurationDao {
    @SqlUpdate("INSERT INTO RoleConfiguration (name, guildId, requiresApproval, grantedTimeInSeconds, approvalChannelId, approvalRoleId) VALUES (:name, :guildId, :requiresApproval, :grantedTimeInSeconds, :approvalChannelId, :approvalRoleId)")
    @GetGeneratedKeys
    long insert(@BindBean RoleConfiguration config);

    @SqlUpdate("UPDATE RoleConfiguration SET name=:name, guildId=:guildId, requiresApproval=:requiresApproval, grantedTimeInSeconds=:grantedTimeInSeconds, approvalChannelId=:approvalChannelId, approvalRoleId=:approvalRoleId WHERE id=:id")
    void update(@BindBean RoleConfiguration config);

    @SqlQuery("SELECT * FROM RoleConfiguration WHERE name=:name AND guildId=:guildId LIMIT 1")
    RoleConfiguration findByNameAndGuild(@Bind("name") String name, @Bind("guildId") long guildId);

    @SqlQuery("SELECT * FROM RoleConfiguration")
    List<RoleConfiguration> findAll();

    @SqlUpdate("DELETE FROM RoleConfiguration WHERE id=:id")
    void delete(@Bind("id") long id);
}

package net.neoforged.discord.bots.pim.dba.dao;

import net.neoforged.discord.bots.pim.dba.model.PendingRoleRequest;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

@RegisterBeanMapper(PendingRoleRequest.class)
public interface PendingRoleRequestDao {
    @SqlUpdate("INSERT INTO PendingRoleRequest (guildId, role, userId, approvalThreadId, reason) VALUES (:guildId, :role, :userId, :approvalThreadId, :reason)")
    @GetGeneratedKeys
    long insert(@BindBean PendingRoleRequest request);

    @SqlUpdate("UPDATE PendingRoleRequest SET guildId=:guildId, role=:role, userId=:userId, approvalThreadId=:approvalThreadId, reason=:reason WHERE id=:id")
    void update(@BindBean PendingRoleRequest request);

    @SqlUpdate("DELETE FROM PendingRoleRequest WHERE id=:id")
    void delete(@Bind("id") long id);

    @SqlQuery("SELECT * FROM PendingRoleRequest WHERE role=:role AND userId=:userId AND guildId=:guildId LIMIT 1")
    PendingRoleRequest findByRoleUserGuild(@Bind("role") String role, @Bind("userId") long userId, @Bind("guildId") long guildId);

    @SqlQuery("SELECT * FROM PendingRoleRequest WHERE id=:id")
    PendingRoleRequest findById(@Bind("id") long id);
}

package net.neoforged.discord.bots.pim.dba.dao;

import net.neoforged.discord.bots.pim.dba.model.RoleRemovalJob;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.*;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import java.util.List;

@RegisterBeanMapper(RoleRemovalJob.class)
public interface RoleRemovalJobDao {
    @SqlUpdate("INSERT INTO RoleRemovalJob (roleName, guildId, roleId, userId, grantedTimeInSeconds, createdAt) VALUES (:roleName, :guildId, :roleId, :userId, :grantedTimeInSeconds, :createdAt)")
    @GetGeneratedKeys
    long insert(@BindBean RoleRemovalJob job);

    @SqlUpdate("DELETE FROM RoleRemovalJob WHERE id=:id")
    void delete(@Bind("id") long id);

    @SqlQuery("SELECT * FROM RoleRemovalJob WHERE createdAt + (grantedTimeInSeconds * 1000) < :now")
    List<RoleRemovalJob> findJobsToRun(@Bind("now") long now);

    @SqlQuery("SELECT * FROM RoleRemovalJob WHERE createdAt + (grantedTimeInSeconds * 1000) > :now")
    List<RoleRemovalJob> findOpenJobs(@Bind("now") long now);

    @SqlQuery("SELECT * FROM RoleRemovalJob WHERE id=:id")
    RoleRemovalJob findById(@Bind("id") long id);
}


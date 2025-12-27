package net.neoforged.discord.bots.pim.dba;

import net.neoforged.discord.bots.pim.dba.dao.PendingRoleRequestDao;
import net.neoforged.discord.bots.pim.dba.dao.RoleConfigurationDao;
import net.neoforged.discord.bots.pim.dba.dao.RoleRemovalJobDao;
import net.neoforged.discord.bots.pim.dba.model.PendingRoleRequest;
import net.neoforged.discord.bots.pim.dba.model.RoleConfiguration;
import net.neoforged.discord.bots.pim.dba.model.RoleRemovalJob;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DBA {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBA.class);
    private static final String DEFAULT_JDBC_URL = "jdbc:sqlite:data/pim.db";

    private final Jdbi jdbi;
    private final RoleConfigurationDao roleConfigDao;
    private final PendingRoleRequestDao pendingRoleRequestDao;
    private final RoleRemovalJobDao roleRemovalJobDao;

    public DBA() {
        this(DEFAULT_JDBC_URL);
    }

    public DBA(String jdbcUrl) {
        this.jdbi = Jdbi.create(jdbcUrl);
        this.jdbi.installPlugin(new SqlObjectPlugin());
        // Ensure tables exist (in production, use migrations)
        this.jdbi.useHandle(handle -> {
            handle.execute("CREATE TABLE IF NOT EXISTS RoleConfiguration (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, guildId INTEGER, requiresApproval INTEGER, grantedTimeInSeconds INTEGER, approvalChannelId INTEGER, approvalRoleId INTEGER)");
            handle.execute("CREATE TABLE IF NOT EXISTS PendingRoleRequest (id INTEGER PRIMARY KEY AUTOINCREMENT, guildId INTEGER, role TEXT, userId INTEGER, approvalThreadId INTEGER, reason TEXT)");
            handle.execute("CREATE TABLE IF NOT EXISTS RoleRemovalJob (id INTEGER PRIMARY KEY AUTOINCREMENT, roleName TEXT, guildId INTEGER, roleId INTEGER, userId INTEGER, grantedTimeInSeconds INTEGER, createdAt INTEGER)");
        });
        this.roleConfigDao = jdbi.onDemand(RoleConfigurationDao.class);
        this.pendingRoleRequestDao = jdbi.onDemand(PendingRoleRequestDao.class);
        this.roleRemovalJobDao = jdbi.onDemand(RoleRemovalJobDao.class);
        LOGGER.info("Database connection established (JDBI)");
    }

    @Nullable
    public RoleConfiguration getRoleConfiguration(String name, long guildId) {
        LOGGER.debug("Getting role configuration for name {},  {}", name, guildId);
        return roleConfigDao.findByNameAndGuild(name, guildId);
    }

    @NotNull
    public PendingRoleRequest createPendingRoleRequest(String role, long userId, String reason, long guildId) {
        LOGGER.debug("Creating new pending role request for role {}, {}, {}, {}", role, userId, reason, guildId);
        PendingRoleRequest request = new PendingRoleRequest();
        request.setRole(role);
        request.setUserId(userId);
        request.setReason(reason);
        request.setGuildId(guildId);
        request.setApprovalThreadId(-1);
        long id = pendingRoleRequestDao.insert(request);
        request.setId(id);
        return request;
    }

    public void updatePendingRoleRequest(PendingRoleRequest request) {
        LOGGER.debug("Updating pending role request for role {}, {}, {}, {}, {}", request.getRole(), request.getGuildId(), request.getUserId(), request.getReason(), request.getApprovalThreadId());
        pendingRoleRequestDao.update(request);
    }

    public void deletePendingRoleRequest(PendingRoleRequest request) {
        LOGGER.debug("Deleting pending role request: {}, {}, {}", request.getRole(), request.getUserId(), request.getGuildId());
        pendingRoleRequestDao.delete(request.getId());
    }

    @Nullable
    public PendingRoleRequest getPendingRoleRequest(String role, long userId, long guildId) {
        LOGGER.debug("Getting pending role request for role {}, {}, {}", role, userId, guildId);
        return pendingRoleRequestDao.findByRoleUserGuild(role, userId, guildId);
    }

    public void createRoleConfiguration(String name, long guildId, boolean requiresApproval, int grantedTimeInSeconds, long approvalChannelId, long approvesRoleId) {
        LOGGER.debug("Creating role configuration for: {}, {}, {}, {}, {}, {}", name, guildId, requiresApproval, grantedTimeInSeconds, approvalChannelId, approvesRoleId);
        RoleConfiguration configuration = new RoleConfiguration();
        configuration.setName(name);
        configuration.setGuildId(guildId);
        configuration.setRequiresApproval(requiresApproval);
        configuration.setGrantedTimeInSeconds(grantedTimeInSeconds);
        configuration.setApprovalChannelId(approvalChannelId);
        configuration.setApprovalRoleId(approvesRoleId);
        roleConfigDao.insert(configuration);
    }

    public List<RoleConfiguration> getRoleConfigurations() {
        LOGGER.debug("Retrieving all role configurations");
        return roleConfigDao.findAll();
    }

    public PendingRoleRequest getPendingRoleRequestById(long id) {
        LOGGER.debug("Getting pending role request for id {}", id);
        return pendingRoleRequestDao.findById(id);
    }

    public void createRoleRemovalJob(String roleName, long guildId, long roleId, long userId, int grantedTimeInSeconds) {
        LOGGER.debug("Adding role removal job for role {}, {}, {}, {}, {}", roleName, guildId, roleId, userId, grantedTimeInSeconds);
        RoleRemovalJob job = new RoleRemovalJob();
        job.setRoleName(roleName);
        job.setGuildId(guildId);
        job.setRoleId(roleId);
        job.setUserId(userId);
        job.setGrantedTimeInSeconds(grantedTimeInSeconds);
        job.setCreatedAt(System.currentTimeMillis());
        roleRemovalJobDao.insert(job);
    }

    public List<RoleRemovalJob> getRemovalJobsToRun() {
        LOGGER.debug("Getting role removal jobs to run: {}", System.currentTimeMillis());
        return roleRemovalJobDao.findJobsToRun(System.currentTimeMillis());
    }

    public List<RoleRemovalJob> getOpenRemovalJobs() {
        LOGGER.debug("Getting open removal jobs: {}", System.currentTimeMillis());
        return roleRemovalJobDao.findOpenJobs(System.currentTimeMillis());
    }

    public void removeRemovalJob(RoleRemovalJob job) {
        LOGGER.debug("Removing role removal job {}", job.toString());
        roleRemovalJobDao.delete(job.getId());
    }

    public void updateRoleConfiguration(RoleConfiguration existing) {
        LOGGER.debug("Updating role configuration: {}", existing.getName());
        roleConfigDao.update(existing);
    }

    public void deleteRoleConfiguration(long id) {
        LOGGER.debug("Deleting role configuration with id {}", id);
        roleConfigDao.delete(id);
    }
}

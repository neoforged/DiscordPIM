package net.neoforged.discord.bots.pim.dba;

import com.github.artbits.jsqlite.DB;
import net.neoforged.discord.bots.pim.dba.model.PendingRoleRequest;
import net.neoforged.discord.bots.pim.dba.model.RoleConfiguration;
import net.neoforged.discord.bots.pim.dba.model.RoleRemovalJob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DBA {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBA.class);

    private final DB db;
    public DBA() {
        this.db = DB.connect("data/pim.db");
        this.db.tables(RoleConfiguration.class, PendingRoleRequest.class, RoleRemovalJob.class);

        LOGGER.info("Database connection established");
    }

    @Nullable
    public RoleConfiguration getRoleConfiguration(String id, long guildId) {
        LOGGER.debug("Getting role configuration for id {},  {}", id, guildId);
        return this.db.first(RoleConfiguration.class, "name = ? && guildId = ?", id, guildId);
    }

    @NotNull
    public PendingRoleRequest createPendingRoleRequest(String role, long userId, String reason, long guildId) {
        LOGGER.info("Creating new pending role request for role {}, {}, {}, {}", role, userId, reason, guildId);
        var request = new PendingRoleRequest(p -> {p.role = role; p.userId = userId; p.reason = reason; p.guildId = guildId;});
        db.insert(request);
        return request;
    }

    public void updatePendingRoleRequest(PendingRoleRequest request) {
        LOGGER.info("Updating pending role request for role {}, {}, {}, {}, {}", request.role, request.guildId, request.userId, request.reason, request.approvalThreadId);
        db.update(request);
    }


    public void deletePendingRoleRequest(PendingRoleRequest request) {
        LOGGER.warn("Deleting pending role request: {}, {}, {}", request.role, request.userId, request.guildId);
        db.delete(PendingRoleRequest.class, request.id());
    }

    @Nullable
    public PendingRoleRequest getPendingRoleRequest(String role, long userId, long guildId) {
        LOGGER.debug("Getting pending role request for role {}, {}, {}", role, userId, guildId);
        return this.db.first(PendingRoleRequest.class, "role = ? && userId = ? && guildId = ?", role, userId, guildId);
    }

    public RoleConfiguration createRoleConfiguration(String name, long guildId, boolean requiresApproval, int grantedTimeInSeconds, long approvalChannelId, long approvesRoleId) {
        LOGGER.warn("Creating role configuration for: {}, {}, {}, {}, {}, {}", name, guildId, requiresApproval, grantedTimeInSeconds, approvalChannelId, approvesRoleId);
        final var configuration = new RoleConfiguration(p -> {
            p.name = name;
            p.guildId = guildId;
            p.requiresApproval = requiresApproval;
            p.grantedTimeInSeconds = grantedTimeInSeconds;
            p.approvalChannelId = approvalChannelId;
            p.approvalRoleId = approvesRoleId;
        });
        db.insert(configuration);
        return configuration;
    }

    public PendingRoleRequest getPendingRoleRequestById(long id) {
        return db.findOne(PendingRoleRequest.class, id);
    }

    public void createRoleRemovalJob(String roleName, long guildId, long roleId, long userId, int grantedTimeInSeconds) {
        LOGGER.warn("Adding role removal job for role {}, {}, {}, {}, {}", roleName, guildId, roleId, userId, grantedTimeInSeconds);
        db.insert(new RoleRemovalJob(p -> {p.roleName = roleName; p.guildId = guildId; p.roleId = roleId; p.userId = userId; p.grantedTimeInSeconds = grantedTimeInSeconds;}));
    }

    public List<RoleRemovalJob> getRemovalJobsToRun() {
        return db.find(RoleRemovalJob.class, options -> {
            options.where("createdAt + (grantedTimeInSeconds * 1000) < ?",  System.currentTimeMillis());
        });
    }

    public void removeRemovalJob(RoleRemovalJob job) {
        db.delete(RoleRemovalJob.class, job.id());
    }
}

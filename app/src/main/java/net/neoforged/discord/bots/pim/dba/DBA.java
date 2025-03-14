package net.neoforged.discord.bots.pim.dba;

import com.github.artbits.jsqlite.DB;
import net.neoforged.discord.bots.pim.dba.model.PendingRoleRequest;
import net.neoforged.discord.bots.pim.dba.model.RoleConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBA {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBA.class);

    private final DB db;
    public DBA() {
        this.db = DB.connect("data/pim.db");
        this.db.tables(RoleConfiguration.class, PendingRoleRequest.class);

        LOGGER.info("Database connection established");
    }

    @Nullable
    public RoleConfiguration getRoleConfiguration(String id) {
        LOGGER.debug("Getting role configuration for id {}", id);
        return this.db.first(RoleConfiguration.class, "name = ?", id);
    }

    @NotNull
    public PendingRoleRequest createPendingRoleRequest(String role, long userId, String reason) {
        LOGGER.info("Creating new pending role request for role {}, {}, {}", role, userId, reason);
        var request = new PendingRoleRequest(p -> {p.role = role; p.userId = userId; p.reason = reason;});
        db.insert(request);
        return request;
    }

    public void updatePendingRoleRequest(PendingRoleRequest request) {
        LOGGER.info("Updating pending role request for role {}, {}, {}, {}", request.role, request.userId, request.reason, request.approvalThreadId);
        db.update(request);
    }


    public void deletePendingRoleRequest(PendingRoleRequest request) {
        LOGGER.warn("Deleting pending role request: {}, {}", request.role, request.userId);
        db.delete(PendingRoleRequest.class, request.id());
    }

    @Nullable
    public PendingRoleRequest getPendingRoleRequest(String role, long userId) {
        LOGGER.debug("Getting pending role request for role {}", role);
        return this.db.first(PendingRoleRequest.class, "role = ? && userId = ?", role, userId);
    }

    public RoleConfiguration createRoleConfiguration(String name, boolean requiresApproval, int grantedTimeInSeconds, long approvalChannelId, long approvesRoleId) {
        LOGGER.warn("Creating role configuration for: {}, {}, {}, {}, {}", name, requiresApproval, grantedTimeInSeconds, approvalChannelId, approvesRoleId);
        final var configuration = new RoleConfiguration(p -> {
            p.name = name;
            p.requiresApproval = requiresApproval;
            p.grantedTimeInSeconds = grantedTimeInSeconds;
            p.approvalChannelId = approvalChannelId;
            p.approvalRoleId = approvesRoleId;
        });
        db.insert(configuration);
        return configuration;
    }
}

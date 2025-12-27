package net.neoforged.discord.bots.pim.service;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.neoforged.discord.bots.pim.dba.DBA;
import net.neoforged.discord.bots.pim.dba.model.RoleRemovalJob;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class IllegalRoleAssignmentService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(IllegalRoleAssignmentService.class);

    private final        DBA    dba;
    @Nullable
    private final String loggingChannelId;

    public IllegalRoleAssignmentService(final DBA dba, @Nullable final String loggingChannelId) {
        this.dba = dba;
        this.loggingChannelId = loggingChannelId;
    }

    public void onStartup(JDA bot) {
        LOGGER.info("Startup role assignment invalidation triggered...");
        var roleConfigurations = dba.getRoleConfigurations();
        if (roleConfigurations.isEmpty()) {
            LOGGER.warn("No role configurations found to invalidate.");
            return;
        } else {
            LOGGER.info("Processing: {} roles...", roleConfigurations.size());
        }

        roleConfigurations.forEach(roleConfig -> {
            LOGGER.info("Validating role configuration: {}", roleConfig.name);
            var guild = bot.getGuildById(roleConfig.guildId);
            if (guild == null) {
                LOGGER.info("Guild with id: {} does not exist!", roleConfig.guildId);
                return;
            }

            var roles = guild.getRolesByName(roleConfig.name, false);
            if (roles.isEmpty()) {
                LOGGER.error("Could not find roles in guild: {} with name: {}", guild.getName(), roleConfig.name);
                return;
            } else {
                LOGGER.info("Checking: {} active roles for invalidation...", roles.size());
            }

            roles.forEach(role -> {
                LOGGER.info("Invalidating role: {}", role.getName());

                var membersWithRole = guild.getMembersWithRoles(role);
                if (membersWithRole.isEmpty()) {
                    LOGGER.info("No members found with role: {}", role.getName());
                } else {
                    LOGGER.warn("Checking: {} for validation...", membersWithRole.size());
                }
                membersWithRole.forEach(member -> {
                    LOGGER.info("Checking member: {} for role: {} in validation...", member.getEffectiveName(), role.getName());
                    checkAndHandle(member.getUser(), role, guild);
                });
            });
        });
        LOGGER.info("Startup role invalidation completed.");
    }

    public void onRoleConfigurationUpserted(final Role role, final Guild guild)
    {
        LOGGER.info("Role configuration upsertion invalidation triggered...");
        guild.getMembersWithRoles(role)
            .forEach(member -> {
                checkAndHandle(member.getUser(), role, guild);
            });
        LOGGER.info("Role configuration upsertion invalidation completed.");
    }

    public void checkAndHandle(User user, Role role, Guild guild) {
        LOGGER.info("Checking role assignment for: {} role: {} in: {}", user.getName(), role.getName(), guild.getName());
        if (!isManagedRole(role))
        {
            LOGGER.info("Role was not a managed role.");
            return;
        }

        if (wasApproved(user, role))
        {
            LOGGER.info("Role was approved");
            return;
        }

        LOGGER.warn("Role was not approved. Unassigning");
        guild.removeRoleFromMember(user, role).queue(
            success -> {
                //Role removed, remove it from the DB.
                LOGGER.warn("Removed unauthorized role from user: {} ({}). Role: {}", user.getIdLong(), user.getName(), role.getName());
            }
        );
        if (loggingChannelId != null) {
            Objects.requireNonNull(guild.getTextChannelById(loggingChannelId))
                .sendMessageEmbeds(new EmbedBuilder()
                    .setTitle("User: " + user.getName() + " tried to add protected role!")
                    .addField("Role", role.getName(), false)
                    .addField("User ID", user.getId(), false)
                    .build())
                .queue();
        }
    }

    private boolean isManagedRole(Role role) {
        return dba.getRoleConfiguration(role.getName(), role.getGuild().getIdLong()) != null;
    }

    private boolean wasApproved(User user, Role role) {
        var openRequests = dba.getOpenRemovalJobs();
        for (final RoleRemovalJob request : openRequests)
        {
            if (request.roleId == role.getIdLong() && request.userId == user.getIdLong()) {
                return true;
            }
        }

        return false;
    }
}

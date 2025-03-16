package net.neoforged.discord.bots.pim.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.neoforged.discord.bots.pim.dba.DBA;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Internal event handler listening for the /pim configure command to configure roles.
 */
public class PimConfigureRequestHandler extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PimConfigureRequestHandler.class);

    private final DBA dba;

    public PimConfigureRequestHandler(DBA dba) {
        this.dba = dba;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("pim") || event.getSubcommandName() == null || !Objects.requireNonNull(event.getSubcommandName()).equals("configure")) {
            LOGGER.debug("Slash command received by PimConfigureRequestHandler");
            return;
        }

        if (event.getMember() == null) {
            LOGGER.warn("PIM Configuration was not send by member!");
            return;
        }

        if (event.getGuild() == null) {
            LOGGER.warn("PIM Configuration was not send by guild!");
            return;
        }

        event.deferReply().queue();

        //Get all parameters.
        final var role = Objects.requireNonNull(event.getOption("role")).getAsRole();
        final var requiresApproval = Objects.requireNonNull(event.getOption("requires-approval")).getAsBoolean();
        final var grantedTimeInSeconds = Objects.requireNonNull(event.getOption("granted-time-in-seconds")).getAsInt();
        final var approvalChannel = Objects.requireNonNull(event.getOption("approval-channel")).getAsChannel();
        final var approversRole = Objects.requireNonNull(event.getOption("approvers-role")).getAsRole();

        //Check if we have an existing.
        final var existing = dba.getRoleConfiguration(role.getName(), event.getGuild().getIdLong());

        if (existing == null) {
            //Create an entirely new configuration
            dba.createRoleConfiguration(role.getName(), role.getGuild().getIdLong(), requiresApproval, grantedTimeInSeconds, approvalChannel.getIdLong(), approversRole.getIdLong());
            event.getHook().editOriginal("PIM Configuration successfully created!").queue();
            LOGGER.info("Role configuration created for: {}, requiring approval: {}, granted for: {} seconds, approved in: {}, by {}",
                    role.getName(),
                    requiresApproval,
                    grantedTimeInSeconds,
                    approvalChannel.getName(),
                    approversRole.getName()
            );
        } else {
            //Update the existing.
            existing.requiresApproval = requiresApproval;
            existing.grantedTimeInSeconds = grantedTimeInSeconds;
            existing.approvalChannelId = approvalChannel.getIdLong();
            existing.approvalRoleId = approversRole.getIdLong();

            dba.updateRoleConfiguration(existing);
            event.getHook().editOriginal("PIM Configuration successfully updated!").queue();

            LOGGER.info("Role configuration updated for: {}, requiring approval: {}, granted for: {} seconds, approved in: {}, by {}",
                    role.getName(),
                    requiresApproval,
                    grantedTimeInSeconds,
                    approvalChannel.getName(),
                    approversRole.getName()
            );
        }


    }
}

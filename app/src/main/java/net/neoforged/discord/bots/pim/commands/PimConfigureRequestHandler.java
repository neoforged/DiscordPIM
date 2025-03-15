package net.neoforged.discord.bots.pim.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.neoforged.discord.bots.pim.dba.DBA;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

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

        final var role = Objects.requireNonNull(event.getOption("role")).getAsRole();
        final var requiresApproval = Objects.requireNonNull(event.getOption("requires-approval")).getAsBoolean();
        final var grantedTimeInSeconds = Objects.requireNonNull(event.getOption("granted-time-in-seconds")).getAsInt();
        final var approvalChannel = Objects.requireNonNull(event.getOption("approval-channel")).getAsChannel();
        final var approversRole = Objects.requireNonNull(event.getOption("approvers-role")).getAsRole();

        dba.createRoleConfiguration(role.getName(), role.getGuild().getIdLong(), requiresApproval, grantedTimeInSeconds, approvalChannel.getIdLong(), approversRole.getIdLong());

        event.getHook().editOriginal("PIM Configuration successfully updated!").queue();
    }
}

package net.neoforged.discord.bots.pim.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.neoforged.discord.bots.pim.dba.DBA;
import net.neoforged.discord.bots.pim.service.EventLoggingService;
import net.neoforged.discord.bots.pim.service.IllegalRoleAssignmentService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Internal event handler listening for the /pim configure command to configure roles.
 */
public class PimUnconfigureRequestHandler extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PimUnconfigureRequestHandler.class);

    private final DBA dba;
    private final EventLoggingService eventLoggingService;

    public PimUnconfigureRequestHandler(DBA dba, final EventLoggingService eventLoggingService) {
        this.dba = dba;
        this.eventLoggingService = eventLoggingService;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("pim") || event.getSubcommandName() == null || !Objects.requireNonNull(event.getSubcommandName()).equals("unconfigure")) {
            LOGGER.debug("Slash command received by PimUnconfigureRequestHandler");
            return;
        }

        if (event.getMember() == null) {
            LOGGER.warn("PIM Unconfiguration was not send by member!");
            return;
        }

        if (event.getGuild() == null) {
            LOGGER.warn("PIM Unconfiguration was not send by guild!");
            return;
        }

        if (!event.getMember().hasPermission(Permission.MANAGE_ROLES)) {
            event.getInteraction().reply("You are need role management permissions to execute this command").queue();
            LOGGER.warn("User: {} tried to configure roles without permission!", event.getMember().getEffectiveName());
            return;
        }

        //Get all parameters.
        final var role = Objects.requireNonNull(event.getOption("role")).getAsRole();

        //Next up are a bunch of DB requests, which can take a bit, lets defer the result.
        event.deferReply().queue();

        //Check if we have an existing.
        final var existing = dba.getRoleConfiguration(role.getName(), event.getGuild().getIdLong());

        if (existing == null) {
            LOGGER.warn("Tried to remove a role configuration which does not exist!");
            event.getHook().editOriginal("This role is not configured!").queue();
        } else {
            //Update the existing.
            dba.deleteRoleConfiguration(existing.getId());
            event.getHook().editOriginal("PIM Configuration for: %s successfully removed!".formatted(existing.name)).queue();
            LOGGER.warn("Role configuration for: {} has been removed!", role.getName());
        }

        eventLoggingService.postEvent(embed -> {
            embed.setTitle("A role has been from from PIM interaction")
                .addField("Role", role.getName(), false)
                .addField("By", event.getMember().getEffectiveName(), false);
        });
    }
}

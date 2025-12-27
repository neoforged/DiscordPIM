package net.neoforged.discord.bots.pim.service;

import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoleAssignmentMonitor extends ListenerAdapter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleAssignmentMonitor.class);

    private final IllegalRoleAssignmentService service;

    public RoleAssignmentMonitor(final IllegalRoleAssignmentService service) {
        this.service = service;
    }

    @Override
    public void onGuildMemberRoleAdd(@NotNull final GuildMemberRoleAddEvent event)
    {
        LOGGER.info("Retrieved role assignment update for: {}", event.getUser().getName());
        //For each role assigned to the user, check if it is managed.
        event.getRoles()
            .forEach(role -> service.checkAndHandle(event.getUser(), role, event.getGuild()));
    }
}

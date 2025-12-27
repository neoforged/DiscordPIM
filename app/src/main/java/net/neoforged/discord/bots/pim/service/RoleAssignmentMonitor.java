package net.neoforged.discord.bots.pim.service;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.neoforged.discord.bots.pim.button.RejectPIMRequestButtonHandler;
import net.neoforged.discord.bots.pim.dba.DBA;
import net.neoforged.discord.bots.pim.dba.model.RoleRemovalJob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

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
        //For each role assigned to the user, check if it is managed.
        event.getRoles()
            .forEach(role -> service.checkAndHandle(event.getUser(), role, event.getGuild()));
    }
}

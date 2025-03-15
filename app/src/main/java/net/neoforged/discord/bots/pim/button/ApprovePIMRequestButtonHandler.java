package net.neoforged.discord.bots.pim.button;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.neoforged.discord.bots.pim.dba.DBA;
import net.neoforged.discord.bots.pim.dba.model.PendingRoleRequest;
import net.neoforged.discord.bots.pim.dba.model.RoleConfiguration;
import net.neoforged.discord.bots.pim.service.RoleAssignmentService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class ApprovePIMRequestButtonHandler extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApprovePIMRequestButtonHandler.class);

    private final DBA dba;
    private final RoleAssignmentService roleAssignmentService;

    public ApprovePIMRequestButtonHandler(DBA dba, RoleAssignmentService roleAssignmentService) {
        this.dba = dba;
        this.roleAssignmentService = roleAssignmentService;
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!Objects.requireNonNull(event.getButton().getId()).startsWith("approve-request/")) {
            LOGGER.debug("Button is not a approve request button");
            return;
        }

        LOGGER.debug("Approve request button pressed");
        event.deferReply().queue(success -> {
            final long requestId = Long.parseLong(event.getButton().getId().substring("approve-request/".length()));
            final PendingRoleRequest request = dba.getPendingRoleRequestById(requestId);

            if (request.userId == Objects.requireNonNull(event.getMember()).getIdLong()) {
                LOGGER.warn("Approve request button pressed");
                event.getHook().editOriginal("You can not approve your own request!").queue();
                return;
            }

            final RoleConfiguration roleConfiguration = dba.getRoleConfiguration(request.role, request.guildId);
            event.getJDA().retrieveUserById(request.userId).queue(user -> {
                roleAssignmentService.assignRoleTo(
                        Objects.requireNonNull(roleConfiguration),
                        user,
                        Objects.requireNonNull(event.getGuild())).queue(success2 -> {
                    event.getHook().editOriginal("Request got approved by: " + Objects.requireNonNull(event.getMember()).getEffectiveName()).queue(
                            success3 -> {
                                event.getChannel().asThreadChannel().getManager().setLocked(true).setArchived(true).queue(
                                        success4 -> {
                                            LOGGER.warn("Role request got approved.");
                                            dba.deletePendingRoleRequest(request);
                                        }
                                );
                            }
                    );
                });
            });

        });
    }
}

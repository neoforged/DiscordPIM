package net.neoforged.discord.bots.pim.button;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.neoforged.discord.bots.pim.dba.DBA;
import net.neoforged.discord.bots.pim.dba.model.PendingRoleRequest;
import net.neoforged.discord.bots.pim.dba.model.RoleConfiguration;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Internal event handler which watches for button presses that indicate the
 */
public class RejectPIMRequestButtonHandler extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RejectPIMRequestButtonHandler.class);

    private final DBA dba;

    public RejectPIMRequestButtonHandler(DBA dba) {
        this.dba = dba;
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!Objects.requireNonNull(event.getButton().getCustomId()).startsWith("reject-request/")) {
            LOGGER.debug("Button is not a reject request button");
            return;
        }

        final long requestId = Long.parseLong(event.getButton().getCustomId().substring("reject-request/".length()));

        LOGGER.debug("Reject button was pressed for request: {}", requestId);
        event.deferReply().queue(success -> {
            final PendingRoleRequest request = dba.getPendingRoleRequestById(requestId);

            //Validate that the user is not self rejecting. It generally won't hurt, but let's just keep the rules clear and simple.
            if (request.userId == Objects.requireNonNull(event.getMember()).getIdLong()) {
                LOGGER.warn("{} attempted to reject their own request: {}", event.getMember().getEffectiveName(), request.role);
                event.getHook().editOriginal("You can not reject your own request!").queue();
                return;
            }
            //Get the configuration for the role that the user is interested in.
            final RoleConfiguration roleConfiguration = dba.getRoleConfiguration(request.role, request.guildId);
            if (roleConfiguration == null) {
                LOGGER.warn("{} attempted to reject a request: {} without a rule configuration", event.getMember().getEffectiveName(), request.role);
                event.getHook().editOriginal("This request can not be rejected. The rule configuration got removed!").queue(success2 -> {
                    event.getJDA().retrieveUserById(request.userId).queue(user -> {
                        //Got the user, cancel and notify.
                        LOGGER.info("Cancelling role request of: {}, for: {}, by: {}", request.role, user.getName(), event.getMember().getEffectiveName());

                        closeAndNotify(event, user, "Your role request for: " + request.role + " has been cancelled.", request);
                    });
                });
                return;
            }

            //Get the user from the central JDA instance.
            //Noteworthy: The event is triggered by a different user, so we can not use its member or user information.
            event.getJDA().retrieveUserById(request.userId).queue(user -> {
                event.getHook().editOriginal("Request got rejected by: " + Objects.requireNonNull(event.getMember()).getEffectiveName()).queue(success2 -> {
                    LOGGER.info("Rejecting role request of: {}, for: {}, by: {}", request.role, user.getName(), event.getMember().getEffectiveName());
                    closeAndNotify(event, user, "Your role request for: " + roleConfiguration.name + " has been rejected.", request);
                });
            });
        });
    }

    private void closeAndNotify(@NotNull ButtonInteractionEvent event, User user, String notificationContent, PendingRoleRequest request) {
        event.getChannel().asThreadChannel().getManager().setLocked(true).setArchived(true).queue(success3 -> {
            user.openPrivateChannel().queue(privateChannel -> {
                privateChannel.sendMessage(notificationContent).queue(success -> LOGGER.info("Notified user: {} of rejected  request: {}", user.getName(), request.role));
            });

            dba.deletePendingRoleRequest(request);

            event.getMessage()
                .editMessageComponents(
                    event.getMessage().getComponentTree()
                        .replace(oldComponent -> {
                            if (oldComponent instanceof Button button)
                            {
                                return button.asDisabled();
                            }

                            return oldComponent;
                        })
                ).queue();
            LOGGER.info("Role request of: {}, for: {}, by: {} has been rejected or cancelled. Processing complete.", request.role, user.getName(), event.getMember().getEffectiveName());
        });
    }
}

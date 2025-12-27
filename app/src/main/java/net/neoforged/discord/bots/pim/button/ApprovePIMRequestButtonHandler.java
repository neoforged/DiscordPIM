package net.neoforged.discord.bots.pim.button;

import net.dv8tion.jda.api.components.Component;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.replacer.ComponentReplacer;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.neoforged.discord.bots.pim.dba.DBA;
import net.neoforged.discord.bots.pim.dba.model.PendingRoleRequest;
import net.neoforged.discord.bots.pim.dba.model.RoleConfiguration;
import net.neoforged.discord.bots.pim.service.RoleAssignmentService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Internal event handler which watches for button presses that indicate the approval of a role request.
 */
public class ApprovePIMRequestButtonHandler extends ListenerAdapter
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ApprovePIMRequestButtonHandler.class);

    private final DBA                   dba;
    private final RoleAssignmentService roleAssignmentService;

    public ApprovePIMRequestButtonHandler(DBA dba, RoleAssignmentService roleAssignmentService)
    {
        this.dba = dba;
        this.roleAssignmentService = roleAssignmentService;
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event)
    {
        if (!Objects.requireNonNull(event.getButton().getCustomId()).startsWith("approve-request/"))
        {
            LOGGER.debug("Button is not a approve request button");
            return;
        }

        final long requestId = Long.parseLong(event.getButton().getCustomId().substring("approve-request/".length()));

        LOGGER.debug("Approve button was pressed for request: {}", requestId);
        event.deferReply().queue(success -> {
            final PendingRoleRequest request = dba.getPendingRoleRequestById(requestId);

            if (request == null)
            {
                LOGGER.debug("Tried to approve request again...");
                event.getHook().editOriginal("Request has already been approved.").queue();
                return;
            }

            //Validate that the user is not self approving.
            if (request.userId == Objects.requireNonNull(event.getMember()).getIdLong())
            {
                LOGGER.warn("{} attempted to approve their own request: {}", event.getMember().getEffectiveName(), request.role);
                event.getHook().editOriginal("You can not approve your own request!").queue();
                return;
            }

            //Get the configuration for the role that the user is interested in.
            final RoleConfiguration roleConfiguration = dba.getRoleConfiguration(request.role, request.guildId);

            //Get the user from the central JDA instance.
            //Noteworthy: The event is triggered by a different user, so we can not use its member or user information.
            event.getJDA().retrieveUserById(request.userId).queue(user -> {
                LOGGER.info("Approving role request of: {}, for: {}, by: {}", request.role, user.getName(), event.getMember().getEffectiveName());

                //Use our internal service to assign the role, this will also create the relevant removal job.
                //And send a DM to the requesting user that his role request has been approved.
                roleAssignmentService.assignRoleTo(
                    Objects.requireNonNull(roleConfiguration),
                    user,
                    Objects.requireNonNull(event.getGuild())).queue(success2 -> {

                    //Notify the approver that the request has been approved.
                    event.getHook().editOriginal("Request got approved by: " + Objects.requireNonNull(event.getMember()).getEffectiveName()).queue(
                        success3 -> {
                            //Lock the thread so that it is preserved for auditing if need be.
                            event.getChannel().asThreadChannel().getManager().setLocked(true).setArchived(true).queue(
                                success4 -> {
                                    //Delete the role request from the DB.
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

                                    LOGGER.info("Role request of: {}, for: {}, by: {} has been approved. Processing complete.",
                                        request.role,
                                        user.getName(),
                                        event.getMember().getEffectiveName());
                                }
                            );
                        }
                    );
                });
            });
        });
    }
}

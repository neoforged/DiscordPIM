package net.neoforged.discord.bots.pim.commands;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.neoforged.discord.bots.pim.dba.DBA;
import net.neoforged.discord.bots.pim.dba.model.PendingRoleRequest;
import net.neoforged.discord.bots.pim.service.RoleAssignmentService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Internal slash command handler which accepts requests and schedules or assigns them based on the role configuration.
 */
public class PimRoleRequestHandler extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PimRoleRequestHandler.class);

    private final DBA dba;
    private final RoleAssignmentService roleAssignmentService;

    public PimRoleRequestHandler(DBA dba, RoleAssignmentService roleAssignmentService) {
        this.dba = dba;
        this.roleAssignmentService = roleAssignmentService;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("pim") || event.getSubcommandName() == null || !Objects.requireNonNull(event.getSubcommandName()).equals("request")) {
            LOGGER.debug("Slash command received by PimRoleRequestHandler");
            return;
        }

        if (event.getMember() == null) {
            LOGGER.warn("PIM Request was not send by member!");
            return;
        }

        if (event.getGuild() == null) {
            LOGGER.warn("PIM Request was not send by guild!");
            return;
        }

        event.deferReply().queue();

        //Get the parameters and the configuration for the role in question.
        final var role = Objects.requireNonNull(event.getOption("role")).getAsRole();
        final var reason = Objects.requireNonNull(event.getOption("reason")).getAsString();
        final var roleConfiguration = dba.getRoleConfiguration(role.getName(), role.getGuild().getIdLong());

        if (roleConfiguration == null) {
            //This role is not managed by this PIM bot.
            LOGGER.warn("Role configuration not found for: {}", role.getName());
            event.getHook().editOriginal("This role is not configured for the use with the pim bot. Please configure it first.").queue();
            return;
        }

        //Check whether we even require approval on the role.
        if (!roleConfiguration.requiresApproval) {
            //No approval required, immediately assign.
            //This will internally send a DM to the user, so that suffices, but to complete the command we also update the original hook.
            roleAssignmentService.assignRoleTo(roleConfiguration, event.getUser(), Objects.requireNonNull(event.getGuild()))
                    .queue(
                            ignored -> event.getHook().editOriginal("Role has been assigned!").queue()
                    );

        } else {
            //Check if we have a pending request.
            var request = dba.getPendingRoleRequest(role.getName(), event.getUser().getIdLong(), role.getGuild().getIdLong());
            if (request != null) {
                //User requested the role twice, reject this second attempt.
                LOGGER.warn("PIM Request was already pending for role {}", role.getName());
                event.getHook().editOriginal("You already have a request pending for this role!").queue();
                return;
            }

            //We have a new request, lets store it in the database.
            request = dba.createPendingRoleRequest(role.getName(), event.getUser().getIdLong(), reason, event.getGuild().getIdLong());

            //Capture the request and then create a private thread within our approval channel.
            var finalRequest = request;
            Objects.requireNonNull(event.getGuild().getTextChannelById(roleConfiguration.approvalChannelId))
                    .createThreadChannel("%s -> %s".formatted(role.getName(), event.getMember().getEffectiveName()), true)
                    .setInvitable(false)
                    .queue(
                            thread -> {
                                //Get the discord role that is allowed to approve the request.
                                var approvalRole = Objects.requireNonNull(thread.getGuild().getRoleById(roleConfiguration.approvalRoleId));

                                //Update the request in the database with the thread information for later processing in button handlers.
                                finalRequest.approvalThreadId = thread.getIdLong();
                                dba.updatePendingRoleRequest(finalRequest);

                                event.getGuild().findMembersWithRoles(approvalRole)
                                        //When we fail to get all members, close the thread and notify the requester.
                                        .onError(error -> onErrorCreatingApprovalThread(event, error, finalRequest))
                                        .onSuccess(members -> {
                                            //Successfully got all required approvers, first post a message in the thread so that it properly is opened.
                                            thread.sendMessage("## PIM Role: " + role.getName() + " has been requested by: " + event.getMember().getEffectiveName() + ".\nPlease validate the request and approve it.\n\n### Reason:\n" + reason)
                                                    .addActionRow(
                                                            //Create the response buttons.
                                                            Button.success("approve-request/" + finalRequest.id(), "Approve Request"),
                                                            Button.danger("reject-request/" + finalRequest.id(), "Reject Request")
                                                    )
                                                    .queue(
                                                            message -> {
                                                                //Now add the approvers to the thread.
                                                                var approvers = new ArrayList<>(List.copyOf(members));
                                                                approvers.removeIf(member -> member.getIdLong() == event.getMember().getIdLong());
                                                                approvers.forEach(approver -> thread.addThreadMember(approver).queue(
                                                                        succes -> LOGGER.debug("Added approver {} to thread {}", approver.getEffectiveName(), thread.getName()),
                                                                        error -> LOGGER.error("Failed to add approver {} to thread {}", approver.getEffectiveName(), thread.getName(), error)
                                                                ));

                                                                //Notify the requester that his or her PIM request has been created and is awaiting approval.
                                                                event.getHook().editOriginal("PIM Request has been created. Please wait for approval!").queue();
                                                            },
                                                            error -> onErrorCreatingApprovalThread(event, error, finalRequest)
                                                    );
                                        });
                            },
                            failure -> onErrorCreatingApprovalThread(event, failure, finalRequest)
                    );
        }
    }

    /**
     * Notifies the requester that we could not properly create the approval thread and as such their request has been cancelled.
     *
     * @param event The slash command event for the request.
     * @param error The error that occurred.
     * @param finalRequest The request made by the user.
     */
    private void onErrorCreatingApprovalThread(@NotNull SlashCommandInteractionEvent event, Throwable error, PendingRoleRequest finalRequest) {
        //Notify the user.
        event.getHook().editOriginal("Unable to create approval thread!" + error.getMessage()).queue();

        //Remove his request from the database.
        dba.deletePendingRoleRequest(finalRequest);

        //If a thread exists, we will now delete it.
        if (finalRequest.approvalThreadId == -1) {
            Objects.requireNonNull(Objects.requireNonNull(event.getGuild()).getThreadChannelById(finalRequest.approvalThreadId)).delete().queue();
        }
    }
}

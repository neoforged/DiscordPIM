package net.neoforged.discord.bots.pim.commands;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.neoforged.discord.bots.pim.dba.DBA;
import net.neoforged.discord.bots.pim.dba.model.PendingRoleRequest;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PimRoleRequestHandler extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PimRoleRequestHandler.class);

    private final DBA dba;

    public PimRoleRequestHandler(DBA dba) {
        this.dba = dba;
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

        final var role = Objects.requireNonNull(event.getOption("role")).getAsRole();
        final var reason = Objects.requireNonNull(event.getOption("reason")).getAsString();
        final var roleConfiguration = dba.getRoleConfiguration(role.getName());

        if (roleConfiguration == null) {
            LOGGER.warn("Role configuration not found for: {}", role.getName());
            event.getHook().editOriginal("This role is not configured for the use with the pim bot. Please configure it first.").queue();
            return;
        }

        if (!roleConfiguration.requiresApproval) {
            event.getGuild().addRoleToMember(event.getMember(), role).queue();
            event.getHook().editOriginal("Role has been assigned!").queue();
        } else {
            var request = dba.getPendingRoleRequest(role.getName(), event.getUser().getIdLong());
            if (request != null) {
                LOGGER.warn("PIM Request was already pending for role {}", role.getName());
                event.getHook().editOriginal("You already have a request pending for this role!").queue();
                return;
            }

            request = dba.createPendingRoleRequest(role.getName(), event.getUser().getIdLong(), reason);

            var finalRequest = request;
            Objects.requireNonNull(event.getGuild().getTextChannelById(roleConfiguration.approvalChannelId)).createThreadChannel("%s -> %s".formatted(role.getName(), event.getMember().getEffectiveName()), true)
                    .setInvitable(false)
                    .queue(
                            thread -> {
                                var approvalRole = Objects.requireNonNull(thread.getGuild().getRoleById(roleConfiguration.approvalRoleId));

                                finalRequest.approvalThreadId = thread.getIdLong();
                                dba.updatePendingRoleRequest(finalRequest);

                                event.getGuild().findMembersWithRoles(approvalRole)
                                        .onError(error -> onErrorCreatingApprovalThread(event, error, finalRequest))
                                        .onSuccess(members -> {
                                            thread.sendMessage("## PIM Role: " + role.getName() + " has been requested by: " + event.getMember().getEffectiveName() + ".\nPlease validate the request and approve it.\n\n### Reason:\n" + reason)
                                                    .addActionRow(
                                                            Button.success("approve-request/" + finalRequest.id(), "Approve Request"),
                                                            Button.danger("reject-request/" + finalRequest.id(), "Reject Request")
                                                    )
                                                    .queue(
                                                            message -> {
                                                                var approvers = new ArrayList<>(List.copyOf(members));
                                                                approvers.removeIf(member -> member.getIdLong() == event.getMember().getIdLong());
                                                                approvers.forEach(approver -> thread.addThreadMember(approver).queue(
                                                                        succes -> LOGGER.debug("Added approver {} to thread {}", approver.getEffectiveName(), thread.getName()),
                                                                        error -> LOGGER.error("Failed to add approver {} to thread {}", approver.getEffectiveName(), thread.getName(), error)
                                                                ));

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

    private void onErrorCreatingApprovalThread(@NotNull SlashCommandInteractionEvent event, Throwable error, PendingRoleRequest finalRequest) {
        event.getHook().editOriginal("Unable to create approval thread!" + error.getMessage()).queue();
        dba.deletePendingRoleRequest(finalRequest);

        if (finalRequest.approvalThreadId == -1) {
            Objects.requireNonNull(Objects.requireNonNull(event.getGuild()).getThreadChannelById(finalRequest.approvalThreadId)).delete().queue();
        }
    }
}

package net.neoforged.discord.bots.pim.button;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.neoforged.discord.bots.pim.dba.DBA;
import net.neoforged.discord.bots.pim.dba.model.PendingRoleRequest;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class RejectPIMRequestButtonHandler extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RejectPIMRequestButtonHandler.class);

    private final DBA dba;

    public RejectPIMRequestButtonHandler(DBA dba) {
        this.dba = dba;
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!Objects.requireNonNull(event.getButton().getId()).startsWith("reject-request/")) {
            LOGGER.debug("Button is not a reject request button");
            return;
        }

        LOGGER.debug("Reject request button pressed");
        event.deferReply().queue();

        final long requestId = Long.parseLong(event.getButton().getId().substring("reject-request/".length()));
        final PendingRoleRequest request = dba.getPendingRoleRequestById(requestId);

        if (request.userId == Objects.requireNonNull(event.getMember()).getIdLong()) {
            LOGGER.warn("Reject request button pressed");
            event.getHook().editOriginal("You can not reject your own request!").queue();
            return;
        }

        event.getHook().editOriginal("Request got rejected by: " + Objects.requireNonNull(event.getMember()).getEffectiveName()).queue();
        event.getChannel().asThreadChannel().getManager().setLocked(true).setArchived(true).queue();

        dba.deletePendingRoleRequest(request);
    }
}

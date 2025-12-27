package net.neoforged.discord.bots.pim.service;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class EventLoggingService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(EventLoggingService.class);

    private final String loggingChannelId;
    private final JDA bot;

    public EventLoggingService(final String loggingChannelId, final JDA bot) {
        this.loggingChannelId = loggingChannelId;
        this.bot = bot;
    }

    public void postEvent(Consumer<EmbedBuilder> builderConsumer) {
        var embedBuilder = new EmbedBuilder();
        builderConsumer.accept(embedBuilder);

        var loggingChannel = bot.getTextChannelById(loggingChannelId);
        if (loggingChannel == null) {
            LOGGER.error("Could not find logging channel with id: {} to send embed: {}", loggingChannelId, embedBuilder.build().toData().toPrettyString());
            return;
        }

        loggingChannel.sendMessageEmbeds(embedBuilder.build()).queue();
    }
}

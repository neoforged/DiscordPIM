package net.neoforged.discord.bots.pim;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.neoforged.discord.bots.pim.commands.CommandRegistrar;
import net.neoforged.discord.bots.pim.commands.PimConfigureRequestHandler;
import net.neoforged.discord.bots.pim.commands.PimRoleRequestHandler;
import net.neoforged.discord.bots.pim.dba.DBA;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PIMBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(PIMBot.class);

    public static void main(String[] args) throws InterruptedException {
        @Nullable
        final var token =  System.getenv("DISCORD_TOKEN");
        if (token == null) {
            LOGGER.error("Environment variable DISCORD_TOKEN has not been set");
            throw new IllegalStateException("Environment variable DISCORD_TOKEN has not been set");
        }

        final var dba = new DBA();

        final var bot = JDABuilder.createDefault(token)
                .enableIntents(
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new PimRoleRequestHandler(dba))
                .addEventListeners(new PimConfigureRequestHandler(dba))
                .build().awaitReady();

        CommandRegistrar.register(bot);
    }
}

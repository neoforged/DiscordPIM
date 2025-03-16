package net.neoforged.discord.bots.pim;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.neoforged.discord.bots.pim.button.ApprovePIMRequestButtonHandler;
import net.neoforged.discord.bots.pim.button.RejectPIMRequestButtonHandler;
import net.neoforged.discord.bots.pim.commands.CommandRegistrar;
import net.neoforged.discord.bots.pim.commands.PimConfigureRequestHandler;
import net.neoforged.discord.bots.pim.commands.PimRoleRequestHandler;
import net.neoforged.discord.bots.pim.dba.DBA;
import net.neoforged.discord.bots.pim.service.JobExecutionService;
import net.neoforged.discord.bots.pim.service.RoleAssignmentService;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PIMBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(PIMBot.class);

    public static void main(String[] args) throws InterruptedException {
        @Nullable final var token = System.getenv("DISCORD_TOKEN");
        if (token == null) {
            LOGGER.error("Environment variable DISCORD_TOKEN has not been set");
            throw new IllegalStateException("Environment variable DISCORD_TOKEN has not been set");
        } else if (token.equals("not_set")) {
            LOGGER.error("Environment variable DISCORD_TOKEN has not been set in a containerized environment. Holding the process forever.");
            //noinspection InfiniteLoopStatement -> The user has not set the correct environment variables in their container environment, we do not want the container to crash over and over again so we hold it here with the previous warning.
            while(true){
                //noinspection BusyWait -> We want to never leave this process.
                Thread.sleep(1000);
            }
        }

        //Create the DB manager.
        final var dba = new DBA();

        //Create our central services.
        final var roleAssignmentService = new RoleAssignmentService(dba);

        //Create the bot.
        final var bot = JDABuilder.createDefault(token)
                .enableIntents(
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.MESSAGE_CONTENT)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .addEventListeners(new PimRoleRequestHandler(dba, roleAssignmentService))
                .addEventListeners(new PimConfigureRequestHandler(dba))
                .addEventListeners(new ApprovePIMRequestButtonHandler(dba, roleAssignmentService))
                .addEventListeners(new RejectPIMRequestButtonHandler(dba))
                .setAutoReconnect(true)
                .setStatus(OnlineStatus.INVISIBLE)
                .setActivity(Activity.customStatus("Managing permissions..."))
                .build().awaitReady();

        //Register the background handler, we can not do this in the builder as we need a JDA reference.
        bot.addEventListener(new JobExecutionService(dba, bot));

        CommandRegistrar.register(bot);
    }
}

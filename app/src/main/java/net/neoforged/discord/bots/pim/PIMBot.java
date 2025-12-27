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
import net.neoforged.discord.bots.pim.service.EventLoggingService;
import net.neoforged.discord.bots.pim.service.IllegalRoleAssignmentService;
import net.neoforged.discord.bots.pim.service.JobExecutionService;
import net.neoforged.discord.bots.pim.service.RoleAssignmentMonitor;
import net.neoforged.discord.bots.pim.service.RoleAssignmentService;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PIMBot
{

    private static final Logger LOGGER = LoggerFactory.getLogger(PIMBot.class);

    public static void main(String[] args) throws InterruptedException
    {
        @Nullable final var token = System.getenv("DISCORD_TOKEN");
        if (token == null)
        {
            LOGGER.error("Environment variable DISCORD_TOKEN has not been set");
            throw new IllegalStateException("Environment variable DISCORD_TOKEN has not been set");
        }
        else if (token.equals("not_set"))
        {
            LOGGER.error("Environment variable DISCORD_TOKEN has not been set in a containerized environment. Holding the process forever.");
            //noinspection InfiniteLoopStatement -> The user has not set the correct environment variables in their container environment, we do not want the container to crash over and over again so we hold it here with the previous warning.
            while (true)
            {
                //noinspection BusyWait -> We want to never leave this process.
                Thread.sleep(1000);
            }
        }

        @Nullable final var loggingChannel = System.getenv("PIM_LOG_CHANNEL");
        if (loggingChannel == null) {
            LOGGER.error("Environment variable PIM_LOG_CHANNEL has not been set");
            throw new IllegalStateException("Environment variable PIM_LOG_CHANNEL has not been set");
        } else {
            LOGGER.warn("Logging critical events to channel with id: {}", loggingChannel);
        }

        //Create the DB manager.
        final var dba = new DBA();

        //Create the bot.
        final var bot = JDABuilder.createDefault(token)
            .enableIntents(
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.MESSAGE_CONTENT)
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .setAutoReconnect(true)
            .setStatus(OnlineStatus.INVISIBLE)
            .setActivity(Activity.customStatus("Managing permissions..."))
            .build().awaitReady();

        //Create our central services.
        final var eventLoggingService = new EventLoggingService(loggingChannel, bot);
        final var roleAssignmentService = new RoleAssignmentService(dba, eventLoggingService);
        final var illegalAssignmentService = new IllegalRoleAssignmentService(dba, eventLoggingService);

        //Register the event handlers
        bot.addEventListener(new PimRoleRequestHandler(dba, roleAssignmentService, eventLoggingService));
        bot.addEventListener(new PimConfigureRequestHandler(dba, illegalAssignmentService, eventLoggingService));
        bot.addEventListener(new ApprovePIMRequestButtonHandler(dba, roleAssignmentService, eventLoggingService));
        bot.addEventListener(new RejectPIMRequestButtonHandler(dba, eventLoggingService));
        bot.addEventListener(new RoleAssignmentMonitor(illegalAssignmentService));
        bot.addEventListener(new JobExecutionService(dba, bot, eventLoggingService));

        CommandRegistrar.register(bot);

        //Once we are started we check all roles we have under control and remove the assignments if they are not requested.
        illegalAssignmentService.onStartup(bot);
    }
}

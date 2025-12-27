package net.neoforged.discord.bots.pim.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal handler which registers all commands that this bot supports.
 */
public class CommandRegistrar
{

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandRegistrar.class);

    public static void register(JDA bot)
    {
        bot.updateCommands()
            .addCommands(
                Commands.slash("pim", "Gives access to the Privileged Identity Management system.")
                    .addSubcommands(
                        new SubcommandData("request", "Allows you to request a role.")
                            .addOption(OptionType.ROLE, "role", "The role to request.", true, false)
                            .addOption(OptionType.STRING, "reason", "The reason you need the role.", true, false),
                        new SubcommandData("configure", "Allows you to configure the role request configuration.")
                            .addOption(OptionType.ROLE, "role", "The role to configure.", true, false)
                            .addOption(OptionType.BOOLEAN, "requires-approval", "Whether the role request requires approval.", true, false)
                            .addOption(OptionType.INTEGER, "granted-time-in-seconds", "The  granted time in seconds the role is granted for.", true, true)
                            .addOption(OptionType.CHANNEL, "approval-channel", "The channel which will contain the approval threads.", true, false)
                            .addOption(OptionType.ROLE, "approvers-role", "The approvers role who's members can approve the request.", true, false),
                        new SubcommandData("unconfigure", "Allows you to remove a role request configuration")
                            .addOption(OptionType.ROLE, "role", "The role to unconfigure", true, true)
                    )
            ).queue(
                success -> LOGGER.info("Registered commands to Discord!"),
                error -> LOGGER.error("Failed to register commands to Discord!", error)
            );
    }
}

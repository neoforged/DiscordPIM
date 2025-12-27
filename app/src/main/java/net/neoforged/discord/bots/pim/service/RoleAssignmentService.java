package net.neoforged.discord.bots.pim.service;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.RestAction;
import net.neoforged.discord.bots.pim.dba.DBA;
import net.neoforged.discord.bots.pim.dba.model.RoleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal service which handles role assignment logic.
 */
public class RoleAssignmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoleAssignmentService.class);

    private final DBA dba;

    public RoleAssignmentService(DBA dba) {
        this.dba = dba;
    }

    /**
     * Prepares a rest action on the Discord API which assigns the given user the given role.
     *
     * @param roleConfiguration The role configuration to assign.
     * @param user The user to assign the role to.
     * @param guild The guild to assign in.
     * @return The rest action, which still needs to be scheduled.
     */
    public RestAction<Void> assignRoleTo(RoleConfiguration roleConfiguration, User user, Guild guild) {
        //Get the role by its name.
        final Role role = guild.getRolesByName(roleConfiguration.name,  false).getFirst();

        if (roleConfiguration.grantedTimeInSeconds <= 0) {
            throw new IllegalStateException("Tried to assign an invalid not assignable role!");
        }


        //The database will automatically set a created timestamp which we use to check if it has run out using the granted time in seconds every minute.
        dba.createRoleRemovalJob(role.getName(), roleConfiguration.guildId, role.getIdLong(), user.getIdLong(), roleConfiguration.grantedTimeInSeconds);

        //Add the role to the member within the guild in question.
        return guild.addRoleToMember(user, role)
                .onSuccess(ignored -> {
                    //Notify the user that his or her role has been added.
                    user.openPrivateChannel().queue(
                            channel -> {
                                channel.sendMessage("Your request for the " + role.getName() + " role has been successfully assigned to you.").queue(
                                        success -> LOGGER.debug("Successfully notified the user {} of his role {}", user.getName(), role.getName())
                                );
                            }
                    );
                });
    }
}

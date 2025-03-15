package net.neoforged.discord.bots.pim.service;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.RestAction;
import net.neoforged.discord.bots.pim.dba.DBA;
import net.neoforged.discord.bots.pim.dba.model.RoleConfiguration;

public class RoleAssignmentService {

    private final DBA dba;

    public RoleAssignmentService(DBA dba) {
        this.dba = dba;
    }

    public RestAction<Void> assignRoleTo(RoleConfiguration roleConfiguration, User user, Guild guild) {
        final Role role = guild.getRolesByName(roleConfiguration.name,  false).getFirst();
        return guild.addRoleToMember(user, role)
                .onSuccess(ignored -> {
                    if (roleConfiguration.grantedTimeInSeconds > 0) {
                        dba.createRoleRemovalJob(role.getName(), roleConfiguration.guildId, role.getIdLong(), user.getIdLong(), roleConfiguration.grantedTimeInSeconds);
                    }
                });
    }
}

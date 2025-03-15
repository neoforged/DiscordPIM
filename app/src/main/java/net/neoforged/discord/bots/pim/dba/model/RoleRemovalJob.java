package net.neoforged.discord.bots.pim.dba.model;

import com.github.artbits.jsqlite.DataSupport;

import java.util.function.Consumer;

public class RoleRemovalJob extends DataSupport<RoleRemovalJob> {

    public String roleName;
    public long guildId;
    public long roleId;
    public long userId;
    public int grantedTimeInSeconds;

    public RoleRemovalJob(Consumer<RoleRemovalJob> consumer) {
        super(consumer);
    }
}

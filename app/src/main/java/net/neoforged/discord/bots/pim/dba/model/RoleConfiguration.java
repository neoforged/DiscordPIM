package net.neoforged.discord.bots.pim.dba.model;

import com.github.artbits.jsqlite.DataSupport;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class RoleConfiguration extends DataSupport<RoleConfiguration> {

    public String name;
    public long guildId;
    public boolean requiresApproval;
    public int grantedTimeInSeconds;
    public long approvalChannelId;
    public long approvalRoleId;

    public RoleConfiguration(Consumer<RoleConfiguration> consumer) {
        super(consumer);
    }


}

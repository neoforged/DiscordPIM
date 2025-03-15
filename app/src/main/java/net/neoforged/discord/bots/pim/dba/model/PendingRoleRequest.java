package net.neoforged.discord.bots.pim.dba.model;

import com.github.artbits.jsqlite.DataSupport;

import java.util.function.Consumer;

public class PendingRoleRequest extends DataSupport<PendingRoleRequest> {

    public long guildId;
    public String role;
    public long userId;
    public long approvalThreadId = -1;
    public String reason;

    public PendingRoleRequest(Consumer<PendingRoleRequest> consumer) {
        super(consumer);
    }
}

package net.neoforged.discord.bots.pim.service;

import com.coreoz.wisp.Scheduler;
import com.coreoz.wisp.schedule.Schedules;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.neoforged.discord.bots.pim.dba.DBA;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;

public class JobExecutionService implements EventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobExecutionService.class);

    private final Scheduler scheduler = new Scheduler();
    private final DBA dba;
    private final JDA jda;

    public JobExecutionService(DBA dba, JDA jda) {
        this.dba = dba;
        this.jda = jda;

        scheduler.schedule(
                "PIM-Jobs",
                this::runJobs,
                Schedules.fixedDelaySchedule(Duration.ofMinutes(1))
        );
    }

    private void runJobs() {
        final var removalJobsToRun = dba.getRemovalJobsToRun();
        removalJobsToRun.forEach(job -> {
            final var guild = jda.getGuildById(job.guildId);
            Objects.requireNonNull(guild).retrieveMemberById(job.userId).queue(member -> {
                final var role = guild.getRoleById(job.roleId);
                if (member.getRoles().contains(role)) {
                    guild.removeRoleFromMember(member, Objects.requireNonNull(role)).queue(
                            success -> {
                                LOGGER.warn("Removed role {} from member {}", job.roleId, member);
                                dba.removeRemovalJob(job);
                            }
                    );
                }
            });
        });
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof ShutdownEvent shutdownEvent) {
            scheduler.gracefullyShutdown();
        }
    }
}

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

/**
 * Internal background service which regularly checks if there are any jobs to run.
 * Currently only runs role removal jobs.
 */
public class JobExecutionService implements EventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobExecutionService.class);

    private final Scheduler scheduler = new Scheduler();
    private final DBA dba;
    private final JDA jda;
    private final EventLoggingService eventLoggingService;

    public JobExecutionService(DBA dba, JDA jda, final EventLoggingService eventLoggingService) {
        this.dba = dba;
        this.jda = jda;
        this.eventLoggingService = eventLoggingService;

        //We run exactly one minute after the completion of our last job.
        scheduler.schedule(
                "PIM-Jobs",
                this::runJobs,
                Schedules.fixedDelaySchedule(Duration.ofMinutes(1))
        );
    }

    private void runJobs() {
        //Get all jobs to remove.
        final var removalJobsToRun = dba.getRemovalJobsToRun();
        removalJobsToRun.forEach(job -> {
            //Get the guild in which we operate from the job, and the member of that guild of which we need to remove the role.
            final var guild = jda.getGuildById(job.guildId);
            Objects.requireNonNull(guild).retrieveMemberById(job.userId).queue(member -> {
                //Get the role.
                final var role = guild.getRoleById(job.roleId);

                //Check if the member still has the role.
                if (member.getRoles().contains(role)) {
                    //User still has the role, schedule the removal on discords API.
                    guild.removeRoleFromMember(member, Objects.requireNonNull(role)).queue(
                            success -> {
                                //Role removed, remove it from the DB.
                                LOGGER.warn("Removed role {} from member {}", job.roleId, member);
                                dba.removeRemovalJob(job);

                                //And last but not least post a log event
                                eventLoggingService.postEvent(embed -> embed.setTitle("A role request has run out and the role has been disabled")
                                    .addField("Role", role.getName(), false)
                                    .addField("User", member.getEffectiveName(), false)
                                );
                            }
                    );
                } else {
                    //User already relinquished the role, so lets remove the job.
                    dba.removeRemovalJob(job);
                }
            });
        });
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof ShutdownEvent) {
            //We monitor for shutdown events, this has two advantages:
            //1) We are properly held in memory so our scheduler can properly live on because we are in the event handler list.
            //2) Our background threads are properly stopped when the bot stops.
            scheduler.gracefullyShutdown();
        }
    }
}

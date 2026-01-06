package com.ocmsintranet.cronservice.framework.workflows.autorevival.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.autorevival.services.AutoRevivalService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for Auto-Revival workflow
 *
 * Triggers the auto-revival job daily at 2:00 AM to automatically revive
 * suspended notices when their due_date_of_revival is reached
 *
 * Schedule: Daily at 2:00 AM (0 0 2 * * ?)
 * ShedLock: Prevents concurrent execution across multiple instances
 */
@Slf4j
@Component
public class AutoRevivalScheduler {

    private final AutoRevivalService autoRevivalService;

    @Value("${cron.autorevival.enabled:false}")
    private boolean enabled;

    public AutoRevivalScheduler(AutoRevivalService autoRevivalService) {
        this.autoRevivalService = autoRevivalService;
    }

    /**
     * Scheduled execution of auto-revival job
     *
     * Runs daily at 2:00 AM
     * ShedLock ensures only one instance runs at a time
     * - lockAtLeastFor: 5 minutes (prevents rapid re-execution)
     * - lockAtMostFor: 30 minutes (releases lock if instance crashes)
     */
    @Scheduled(cron = "${cron.autorevival.schedule:0 0 2 * * ?}")
    @SchedulerLock(
        name = "suspension_auto_revival",
        lockAtLeastFor = "PT5M",
        lockAtMostFor = "PT30M"
    )
    public void runAutoRevival() {
        if (!enabled) {
            log.debug("Auto-revival cron is disabled. Skipping execution.");
            return;
        }

        log.info("Starting scheduled auto-revival job execution");

        try {
            autoRevivalService.executeJob()
                .thenAccept(result -> {
                    log.info("Auto-revival job completed successfully: {}", result);
                })
                .exceptionally(e -> {
                    log.error("Error in scheduled auto-revival job: {}", e.getMessage(), e);
                    return null;
                });

        } catch (Exception e) {
            log.error("Unexpected error triggering auto-revival job: {}", e.getMessage(), e);
        }
    }
}

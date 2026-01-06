package com.ocmsintranet.cronservice.framework.workflows.agencies.ces.schedulers;

import com.ocmsintranet.cronservice.framework.services.ces.CesDownloadAttachmentService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class CesDownloadAttachmentScheduler {

    private final CesDownloadAttachmentService cesDownloadAttachmentService;
    
    @Value("${cron.ces.attachment.enabled:false}")
    private boolean enabled;

    public CesDownloadAttachmentScheduler(CesDownloadAttachmentService cesDownloadAttachmentService) {
        this.cesDownloadAttachmentService = cesDownloadAttachmentService;
    }


    @Scheduled(cron = "${cron.ces.attachment.schedule:}")
    @SchedulerLock(name = "download_ces_photos", lockAtLeastFor = "PT5M", lockAtMostFor = "PT20M")
    public void executeScheduledJob() {
        if (!enabled) {
            log.info("CES Download Attachment scheduler is disabled. Skipping execution.");
            return;
        }

        log.info("Starting scheduled CES Download Attachment processing");
        try {
            cesDownloadAttachmentService.executeCesDownloadAttachmentFunction();
            log.info("Successfully completed CES Download Attachment processing");
        } catch (Exception e) {
            log.error("Error during CES Download Attachment processing: {}", e.getMessage(), e);
        }
    }
}

package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity for ocms_change_of_processing table
 * Audit trail for Change Processing Stage operations
 * Based on OCMS CPS Spec §4.5
 *
 * Uses composite primary key: (notice_no, date_of_change, new_processing_stage)
 * This allows multiple stage changes per notice over time while preventing
 * duplicate changes to the same stage on the same day (idempotency).
 */
@Entity
@Table(name = "ocms_change_of_processing", schema = "ocmsizmgr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(OcmsChangeOfProcessing.CompositeKey.class)
public class OcmsChangeOfProcessing {

    @Id
    @Column(name = "notice_no", nullable = false, length = 10)
    private String noticeNo;

    @Id
    @Column(name = "date_of_change", nullable = false)
    private LocalDate dateOfChange;

    @Id
    @Column(name = "new_processing_stage", nullable = false, length = 3)
    private String newProcessingStage;

    @Column(name = "last_processing_stage", length = 3)
    private String lastProcessingStage;

    @Column(name = "reason_of_change", length = 3)
    private String reasonOfChange;

    @Column(name = "authorised_officer", length = 50)
    private String authorisedOfficer;

    @Column(name = "source", length = 8)
    private String source;

    @Column(name = "remarks", length = 200)
    private String remarks;

    @Column(name = "cre_date", nullable = false)
    private LocalDateTime creDate;

    @Column(name = "cre_user_id", nullable = false, length = 10)
    private String creUserId;

    @Column(name = "upd_date")
    private LocalDateTime updDate;

    @Column(name = "upd_user_id", length = 50)
    private String updUserId;

    @PrePersist
    protected void onCreate() {
        if (creDate == null) {
            creDate = LocalDateTime.now();
        }
        if (dateOfChange == null) {
            dateOfChange = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updDate = LocalDateTime.now();
    }

    /**
     * Composite primary key class for OcmsChangeOfProcessing
     * Required by JPA when using @IdClass annotation
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeKey implements Serializable {
        private static final long serialVersionUID = 1L;

        private String noticeNo;
        private LocalDate dateOfChange;
        private String newProcessingStage;
    }
}

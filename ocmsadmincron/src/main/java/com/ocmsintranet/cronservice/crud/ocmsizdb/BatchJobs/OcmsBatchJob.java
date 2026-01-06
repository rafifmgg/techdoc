package com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs;

import com.ocmsintranet.cronservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ocms_batch_job" ,schema = "ocmsizmgr")
@NoArgsConstructor
@AllArgsConstructor
public class OcmsBatchJob extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_job_id", nullable = false)
    private Integer batchJobId;

    @Column(name = "name", length = 64, nullable = false)
    private String name;

    @Column(name = "run_status", length = 1)
    private String runStatus;

    @Column(name = "log_text", columnDefinition = "TEXT", nullable = false)
    private String logText;

    @Column(name = "start_run", nullable = false)
    private LocalDateTime startRun;

    @Column(name = "end_run")
    private LocalDateTime endRun;

}

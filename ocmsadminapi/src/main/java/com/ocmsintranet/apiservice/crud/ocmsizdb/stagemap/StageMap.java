package com.ocmsintranet.apiservice.crud.ocmsizdb.stagemap;

import com.ocmsintranet.apiservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity for ocms_stage_map table
 * Defines valid stage transitions for workflow
 * Based on OCMS Data Dictionary
 */
@Entity
@Table(name = "ocms_stage_map", schema = "ocmsizmgr")
@IdClass(StageMapId.class)
@Getter
@Setter
@NoArgsConstructor
public class StageMap extends BaseEntity {

    @Id
    @Column(name = "last_processing_stage", length = 3, nullable = false)
    private String lastProcessingStage;

    @Id
    @Column(name = "next_processing_stage", length = 100, nullable = false)
    private String nextProcessingStage;
}

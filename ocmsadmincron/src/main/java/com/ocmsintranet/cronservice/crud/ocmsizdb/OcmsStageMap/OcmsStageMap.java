package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsStageMap;

import com.ocmsintranet.cronservice.crud.BaseEntity;
import com.ocmsintranet.cronservice.crud.annotations.NonEditable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "ocms_stage_map", schema = "ocmsizmgr")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcmsStageMap extends BaseEntity {

    @Id
    @Column(name = "last_processing_stage", length = 3, nullable = false)
    private String lastProcessingStage;

    @Column(name = "next_processing_stage", length = 100, nullable = false)
    private String nextProcessingStage;
}

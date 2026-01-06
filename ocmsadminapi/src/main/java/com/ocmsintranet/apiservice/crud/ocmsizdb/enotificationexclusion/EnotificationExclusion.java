package com.ocmsintranet.apiservice.crud.ocmsizdb.enotificationexclusion;

import com.ocmsintranet.apiservice.crud.BaseEntity;
import com.ocmsintranet.apiservice.crud.annotations.NonEditable;

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

@Entity
@Table(name = "ocms_enotification_exclusion_list", schema = "ocmsizmgr")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnotificationExclusion extends BaseEntity {

    @Column(name = "id_no", nullable = false, length = 20)
    @NotBlank
    @NonEditable
    @Id
    private String idNo;

    @Column(name = "remarks", nullable = false, length = 200)
    @NotBlank
    private String remarks;
}

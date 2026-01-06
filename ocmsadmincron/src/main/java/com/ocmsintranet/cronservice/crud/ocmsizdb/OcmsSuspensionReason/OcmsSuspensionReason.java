package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSuspensionReason;

import com.ocmsintranet.cronservice.crud.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ocms_suspension_reason", schema = "ocmsizmgr")
@Setter
@Getter
@NoArgsConstructor
@IdClass(OcmsSuspensionReasonId.class)
public class OcmsSuspensionReason extends BaseEntity {

    @Column(name = "suspension_type", length = 2, nullable = false)
    @Id
    @NotBlank
    private String suspensionType;

    @Column(name = "reason_of_suspension", length = 3, nullable = false)
    @Id
    @NotBlank
    private String reasonOfSuspension;

    @Column(name = "status", length = 1, nullable = false)
    @NotBlank
    private String status;

    @Column(name = "description", length = 200, nullable = false)
    @NotBlank
    private String description;

    @Column(name = "no_of_days_for_revival", length = 3)
    private Integer noOfDaysForRevival;
}

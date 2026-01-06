package com.ocmsintranet.apiservice.crud.ocmsizdb.suspensionreason;


import com.ocmsintranet.apiservice.crud.BaseEntity;
import com.ocmsintranet.apiservice.crud.annotations.FindAble;
import com.ocmsintranet.apiservice.crud.annotations.InAble;
import com.ocmsintranet.apiservice.crud.annotations.NonEditable;
import com.ocmsintranet.apiservice.crud.annotations.NumberAble;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ocms_suspension_reason")
@Setter
@Getter
@NoArgsConstructor
@IdClass(SuspensionReasonId.class)
public class SuspensionReason extends BaseEntity {

    @Column(name = "suspension_type", length = 2, nullable=false)
    @Id
    @FindAble
    @InAble
    @NotBlank
    @NonEditable
    private String suspensionType;

    @Column(name = "reason_of_suspension", length = 3, nullable=false)
    @Id
    @FindAble
    @InAble
    @NotBlank
    @NonEditable
    private String reasonOfSuspension;

    @Column(name = "status", length = 1, nullable=false)
    @FindAble
    @InAble
    @NotBlank
    private String status;

    @Column(name = "description", length = 200, nullable=false)
    @FindAble
    @InAble
    @NotBlank
    private String description;

    @Column(name = "no_of_days_for_revival", length = 3)
    @NumberAble
    private Integer noOfDaysForRevival;
}

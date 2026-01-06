package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsReduction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ocmsintranet.apiservice.crud.BaseEntity;
import com.ocmsintranet.apiservice.crud.annotations.NonEditable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing the ocms_reduced_offence_amount table.
 * This table stores information about reductions applied to offence notices.
 */
@Entity
@Table(name = "ocms_reduced_offence_amount", schema = "ocmsizmgr")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@jakarta.persistence.IdClass(OcmsReducedOffenceAmountId.class)
public class OcmsReducedOffenceAmount extends BaseEntity {

    @Column(name = "notice_no", length = 10, nullable = false)
    @Id
    @NonEditable
    private String noticeNo;

    @Column(name = "sr_no", nullable = false)
    @Id
    @NonEditable
    private Integer srNo;

    @Column(name = "date_of_reduction")
    private LocalDateTime dateOfReduction;

    @Column(name = "amount_reduced", precision = 19, scale = 2)
    private BigDecimal amountReduced;

    @Column(name = "amount_payable", precision = 19, scale = 2)
    private BigDecimal amountPayable;

    @Column(name = "reason_of_reduction", length = 3)
    private String reasonOfReduction;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "authorised_officer", length = 50)
    private String authorisedOfficer;

    @Column(name = "remarks", length = 200)
    private String remarks;
}

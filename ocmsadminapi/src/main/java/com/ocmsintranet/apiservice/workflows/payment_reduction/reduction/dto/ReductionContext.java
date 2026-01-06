package com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.dto;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal context holder for reduction processing.
 * Contains all necessary data for performing the reduction workflow.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReductionContext {

    /**
     * The original request from external system
     */
    private ReductionRequest request;

    /**
     * The loaded notice entity from database
     */
    private OcmsValidOffenceNotice notice;

    /**
     * Computer rule code from the notice (for eligibility checks)
     */
    private Integer computerRuleCode;

    /**
     * Last processing stage from the notice (for eligibility checks)
     */
    private String lastProcessingStage;

    /**
     * Serial number shared between suspended_notice and reduced_offence_amount.
     * Both tables use the same sr_no for a given reduction transaction.
     */
    private Integer srNo;

    /**
     * Audit information: timestamp when reduction processing started
     */
    private String processingStartTime;

    /**
     * Audit information: any additional context for logging
     */
    private String auditContext;
}

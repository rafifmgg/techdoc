package com.ocmsintranet.apiservice.testing.reduction.helpers;

import org.springframework.stereotype.Component;

/**
 * Context object to hold shared data between reduction test steps
 */
@Component
public class TestContext {

    // Store Step 3 data for Step 4 verification
    private Object intranetVonData;  // Intranet: ocms_valid_offence_notice
    private Object internetVonData;  // Internet: eocms_valid_offence_notice
    private Object suspendedNoticeData;  // Intranet: ocms_suspended_notice
    private Object reducedAmountLogData;  // Intranet: ocms_reduced_offence_amount
    private Object parameterListData;  // Parameter table

    public Object getIntranetVonData() {
        return intranetVonData;
    }

    public void setIntranetVonData(Object intranetVonData) {
        this.intranetVonData = intranetVonData;
    }

    public Object getInternetVonData() {
        return internetVonData;
    }

    public void setInternetVonData(Object internetVonData) {
        this.internetVonData = internetVonData;
    }

    public Object getSuspendedNoticeData() {
        return suspendedNoticeData;
    }

    public void setSuspendedNoticeData(Object suspendedNoticeData) {
        this.suspendedNoticeData = suspendedNoticeData;
    }

    public Object getReducedAmountLogData() {
        return reducedAmountLogData;
    }

    public void setReducedAmountLogData(Object reducedAmountLogData) {
        this.reducedAmountLogData = reducedAmountLogData;
    }

    public Object getParameterListData() {
        return parameterListData;
    }

    public void setParameterListData(Object parameterListData) {
        this.parameterListData = parameterListData;
    }

    /**
     * Clear all stored data
     */
    public void clear() {
        this.intranetVonData = null;
        this.internetVonData = null;
        this.suspendedNoticeData = null;
        this.reducedAmountLogData = null;
        this.parameterListData = null;
    }
}

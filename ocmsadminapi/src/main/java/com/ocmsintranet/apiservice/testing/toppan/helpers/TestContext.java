package com.ocmsintranet.apiservice.testing.toppan.helpers;

import org.springframework.stereotype.Component;

/**
 * Context object to hold shared data between test steps
 */
@Component
public class TestContext {

    // Store Step 3 data for Step 4 verification
    private Object validOffenceNoticeData;
    private Object driverParticularsData;
    private Object driverNoticeData;
    private Object parameterListData;

    public Object getValidOffenceNoticeData() {
        return validOffenceNoticeData;
    }

    public void setValidOffenceNoticeData(Object validOffenceNoticeData) {
        this.validOffenceNoticeData = validOffenceNoticeData;
    }

    public Object getDriverParticularsData() {
        return driverParticularsData;
    }

    public void setDriverParticularsData(Object driverParticularsData) {
        this.driverParticularsData = driverParticularsData;
    }

    public Object getDriverNoticeData() {
        return driverNoticeData;
    }

    public void setDriverNoticeData(Object driverNoticeData) {
        this.driverNoticeData = driverNoticeData;
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
        this.validOffenceNoticeData = null;
        this.driverParticularsData = null;
        this.driverNoticeData = null;
        this.parameterListData = null;
    }
}

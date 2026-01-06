package com.ocmseservice.apiservice.workflows.spcp.model;

import lombok.Data;

/**
 * Request model for retrieving MyInfo data
 */
@Data
public class MyInfoRequest {
    private String appId;
    private String nric;
    private String txnNo;
}

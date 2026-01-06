package com.ocmseservice.apiservice.workflows.spcp.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.*;

import java.io.Serializable;

/**
 * Request model for SingPass/CorpPass authentication
 */
@JsonAutoDetect
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SpcpAuthRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Application id of the e-Service
     */
    private String appId;

    /**
     * Authentication transaction id.
     * This id is parameter returned to the e-Service success url.
     */
    private String authTxnId;
}

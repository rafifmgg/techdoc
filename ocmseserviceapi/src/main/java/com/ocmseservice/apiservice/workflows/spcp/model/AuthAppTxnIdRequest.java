package com.ocmseservice.apiservice.workflows.spcp.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.*;

import java.io.Serializable;

/**
 * Request model for creating application transaction ID
 */
@JsonAutoDetect
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthAppTxnIdRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Session id
     */
    private String sessionId;

    /**
     * Application id of the e-Service
     */
    private String appId;
}

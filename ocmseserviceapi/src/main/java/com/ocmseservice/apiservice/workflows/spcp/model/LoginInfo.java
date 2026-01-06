package com.ocmseservice.apiservice.workflows.spcp.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.*;

import java.io.Serializable;

/**
 * Model for login information including JWT tokens
 */
@JsonAutoDetect
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Response code from the authentication process
     */
    private String responseCode;
    
    /**
     * Response message from the authentication process
     */
    private String responseMsg;
    
    /**
     * JWT authentication token
     */
    private String authToken;
    
    /**
     * JWT refresh token
     */
    private String refreshToken;
    
    /**
     * Username of the authenticated user
     */
    private String userName;
    
    /**
     * NRIC of the authenticated user
     */
    private String nric;
    
    /**
     * Entity ID of the authenticated user
     */
    private String entityId;
    
    /**
     * Entity type of the authenticated user
     */
    private String entityType;
}

package com.ocmseservice.apiservice.workflows.spcp.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.io.Serializable;

/**
 * Response model for SingPass/CorpPass authentication
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SpcpAuthResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Response code from the authentication service
     */
    private String responseCode;
    
    /**
     * Response message from the authentication service
     */
    private String responseMsg;

    /**
     * NRIC, FIN or Foreign ID number of the user.
     */
    private String nric;

    /**
     * Entity ID (UEN or non-UEN ID) of the entity to which the user belongs in Corppass
     */
    private String entityId;

    /**
     * Status of the entity in Corppass as provided by issuance agencies. The possible values for entity
     * status are:
     * 1) Registered
     * 2) De-Registered
     * 3) Withdrawn
     */
    private String entityStatus;

    /**
     * Type of Entity that the user belongs to. E.g. UEN, Non-UEN
     */
    private String entityType;
    
    /**
     * Username of the authenticated user
     */
    private String userName;
}

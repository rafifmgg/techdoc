package com.ocmseservice.apiservice.workflows.spcp.controller;

import com.ocmseservice.apiservice.workflows.spcp.model.*;
import com.ocmseservice.apiservice.workflows.spcp.service.SpcpAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for SingPass/CorpPass authentication
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class SpcpAuthController {

    private final SpcpAuthService spcpAuthService;

    /**
     * Initiate authentication by creating application transaction ID
     * This endpoint is called by the frontend to get a transaction ID for SingPass/CorpPass authentication
     * It calls the external SIT endpoint: https://singpasscorppass.azurewebsites.net/spcpDS/spcp/createAppTxnId
     *
     * @param request AuthAppTxnIdRequest containing sessionId and appId
     * @return AuthAppTxnIdResponse with transaction ID for redirect
     */
    @PostMapping("/spcpDS/spcp/v1/createAppTxnId")
    public ResponseEntity<AuthAppTxnIdResponse> createAppTxnId(@RequestBody AuthAppTxnIdRequest request) {
        log.info("Creating application transaction ID for session: {}", request.getSessionId());
        AuthAppTxnIdResponse response = spcpAuthService.createAppTxnId(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get authentication response from SingPass/CorpPass
     * This endpoint is called by the frontend after redirect from SingPass/CorpPass
     * It calls the external SIT endpoint: https://singpasscorppass.azurewebsites.net/spcpDS/spcp/getAuthResponse
     *
     * @param request SpcpAuthRequest containing appId and authTxnId
     * @return SpcpAuthResponse with authentication details
     */
    @PostMapping("/spcpDS/spcp/v1/getAuthResponse")
    public ResponseEntity<SpcpAuthResponse> getAuthResponse(@RequestBody SpcpAuthRequest request) {
        log.info("Getting authentication response for authTxnId: {}", request.getAuthTxnId());
        SpcpAuthResponse response = spcpAuthService.getAuthResponse(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get MyInfo data from SingPass/CorpPass
     * This endpoint is called by the frontend to retrieve user's MyInfo data
     * It calls the external SIT endpoint: https://singpasscorppass.azurewebsites.net/spcpDS/myinfo/getMyInfoData
     *
     * @param request MyInfoRequest containing appId, nric, and txnNo
     * @return MyInfoResponse with user's personal information
     */
    @PostMapping("/spcpDS/myinfo/v1/getMyInfoData")
    public ResponseEntity<MyInfoResponse> getMyInfoData(@RequestBody MyInfoRequest request) {
        log.info("Getting MyInfo data for NRIC: {}", request.getNric());
        MyInfoResponse response = spcpAuthService.getMyInfoData(request);
        return ResponseEntity.ok(response);
    }
}

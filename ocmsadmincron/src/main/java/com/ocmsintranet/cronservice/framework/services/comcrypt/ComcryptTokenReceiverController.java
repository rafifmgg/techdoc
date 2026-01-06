package com.ocmsintranet.cronservice.framework.services.comcrypt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for receiving COMCRYPT token callbacks.
 *
 * This controller handles callbacks from the COMCRYPT server when a token is ready.
 * It updates the corresponding ComcryptOperation entity with the received token.
 *
 * Note: Response code handling (HC200, HC500, HC408) for encrypt/decrypt operations
 * is handled in ComcryptUtils, not here. This endpoint only receives the token callback.
 */
@Slf4j
@RestController
@RequestMapping("/crypt/token/v1")
@RequiredArgsConstructor
public class ComcryptTokenReceiverController {

    // Changed from ComcryptOperationFrameworkServiceImpl to ComcryptOperationFrameworkService (interface)
    private final ComcryptOperationFrameworkService comcryptOperationFrameworkService;

    /**
     * Endpoint for receiving COMCRYPT token callbacks.
     * 
     * @param payload The callback payload containing requestId and token (appCode is optional)
     * @return Response with success status
     */
    @PostMapping("/receiver")
    public ResponseEntity<Map<String, Object>> receiveToken(@RequestBody Map<String, Object> payload) {
        log.info("Received COMCRYPT token callback: {}", payload);
        
        // Extract callback parameters
        String requestId = (String) payload.get("requestId");
        String token = (String) payload.get("token");
        String appCode = (String) payload.get("appCode");

        // Validate callback parameters
        if (requestId == null || token == null) {
            log.error("Invalid COMCRYPT token callback - missing required parameters");
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Missing required parameters: requestId, token"
            ));
        }
        
        try {
            // Process the token callback using the framework service
            boolean processed = comcryptOperationFrameworkService.processTokenCallback(requestId, token);
            
            if (!processed) {
                log.error("Failed to process token for requestId: {}", requestId);
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Failed to process token for requestId: " + requestId
                ));
            }
            
            log.info("COMCRYPT token processed successfully for requestId: {}", requestId);

            // Return acknowledged response as per diagram
            return ResponseEntity.ok(Map.of(
                "acknowledged", true,
                "appCode", appCode != null ? appCode : "",
                "requestId", requestId
            ));
            
        } catch (Exception e) {
            log.error("Error processing COMCRYPT token callback", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Internal server error: " + e.getMessage()
            ));
        }
    }
}
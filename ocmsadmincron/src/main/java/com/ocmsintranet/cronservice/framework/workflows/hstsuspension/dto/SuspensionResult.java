package com.ocmsintranet.cronservice.framework.workflows.hstsuspension.dto;

import lombok.Data;

/**
 * DTO for HST auto-suspend operation results
 */
@Data
public class SuspensionResult {
    private boolean success;
    private String message;
    private int totalProcessed;
    private int suspended;
    private int skipped;
    private int errors;

    public static SuspensionResult success(int totalProcessed, int suspended, int skipped) {
        SuspensionResult result = new SuspensionResult();
        result.setSuccess(true);
        result.setTotalProcessed(totalProcessed);
        result.setSuspended(suspended);
        result.setSkipped(skipped);
        result.setErrors(0);
        result.setMessage(String.format("Processed %d HST IDs: %d suspended, %d skipped",
                totalProcessed, suspended, skipped));
        return result;
    }

    public static SuspensionResult error(String message) {
        SuspensionResult result = new SuspensionResult();
        result.setSuccess(false);
        result.setMessage(message);
        result.setTotalProcessed(0);
        result.setSuspended(0);
        result.setSkipped(0);
        result.setErrors(1);
        return result;
    }

    public String getDetailedMessage() {
        return String.format("Total: %d, Suspended: %d, Skipped: %d, Errors: %d - %s",
                totalProcessed, suspended, skipped, errors, message);
    }
}

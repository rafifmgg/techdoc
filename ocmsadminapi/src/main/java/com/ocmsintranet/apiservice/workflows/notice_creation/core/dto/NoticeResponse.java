package com.ocmsintranet.apiservice.workflows.notice_creation.core.dto;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ocmsintranet.apiservice.crud.exception.ErrorCodes;

/**
 * DTO class for notice creation response
 * This ensures proper JSON serialization of the data field as an array
 */
public class NoticeResponse {
    private static final Logger log = LoggerFactory.getLogger(NoticeResponse.class);
    
    @JsonProperty("data")
    private ResponseData data;
    
    /**
     * Base class for response data
     */
    public static abstract class ResponseData {
        protected String appCode;
        protected String message;
        
        public String getAppCode() {
            return appCode;
        }
        
        public void setAppCode(String appCode) {
            this.appCode = appCode;
        }
        
        public void setAppCode(ErrorCodes errorCode) {
            this.appCode = errorCode.getAppCode();
            if (this.message == null) {
                this.message = errorCode.getMessage();
            }
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        // Methods for NoticeProcessingHelper compatibility
        public void setNoticeNo(String noticeNo) {
            // Default implementation does nothing
            log.debug("setNoticeNo called on base ResponseData");
        }
        
        public void setNoticesProcessed(String noticesProcessed) {
            // Default implementation does nothing
            log.debug("setNoticesProcessed called on base ResponseData");
        }
        
        public void setNoticesFailed(String noticesFailed) {
            // Default implementation does nothing
            log.debug("setNoticesFailed called on base ResponseData");
        }
        
        public void setFailedNotices(String failedNotices) {
            // Default implementation does nothing
            log.debug("setFailedNotices called on base ResponseData");
        }
        
        public void setDuplicateNoticesCount(String duplicateNoticesCount) {
            // Default implementation does nothing
            log.debug("setDuplicateNoticesCount called on base ResponseData");
        }
        
        public void setDuplicateNotices(String duplicateNotices) {
            // Default implementation does nothing
            log.debug("setDuplicateNotices called on base ResponseData");
        }
        
        public void setTotalNotices(String totalNotices) {
            // Default implementation does nothing
            log.debug("setTotalNotices called on base ResponseData");
        }
        
        public void setNoticesList(List<String> noticesList) {
            // Default implementation does nothing
            log.debug("setNoticesList called on base ResponseData");
        }
    }
    
    /**
     * Response data for single notice
     * JsonInclude.Include.NON_NULL ensures null fields are excluded from JSON
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SingleNoticeResponseData extends ResponseData {
        private String noticeNo;
        
        // These fields are intentionally not defined in this class
        // to ensure they don't appear in the JSON output
        
        public String getNoticeNo() {
            return noticeNo;
        }
        
        @Override
        public void setNoticeNo(String noticeNo) {
            this.noticeNo = noticeNo;
        }
    }
    
    /**
     * Response data for multiple notices
     */
    public static class MultipleNoticeResponseData extends ResponseData {
        private String noticesProcessed;
        private String noticesFailed;
        private String failedNotices;
        private String duplicateNoticesCount;
        private String duplicateNotices;
        private String totalNotices;
        
        @JsonProperty("data")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<String> noticesList = new ArrayList<>();
        
        public String getNoticesProcessed() {
            return noticesProcessed;
        }
        
        @Override
        public void setNoticesProcessed(String noticesProcessed) {
            this.noticesProcessed = noticesProcessed;
        }
        
        public String getNoticesFailed() {
            return noticesFailed;
        }
        
        @Override
        public void setNoticesFailed(String noticesFailed) {
            this.noticesFailed = noticesFailed;
        }
        
        public String getFailedNotices() {
            return failedNotices;
        }
        
        @Override
        public void setFailedNotices(String failedNotices) {
            this.failedNotices = failedNotices;
        }
        
        public String getDuplicateNoticesCount() {
            return duplicateNoticesCount;
        }
        
        @Override
        public void setDuplicateNoticesCount(String duplicateNoticesCount) {
            this.duplicateNoticesCount = duplicateNoticesCount;
        }
        
        public String getDuplicateNotices() {
            return duplicateNotices;
        }
        
        @Override
        public void setDuplicateNotices(String duplicateNotices) {
            this.duplicateNotices = duplicateNotices;
        }
        
        public String getTotalNotices() {
            return totalNotices;
        }
        
        @Override
        public void setTotalNotices(String totalNotices) {
            this.totalNotices = totalNotices;
        }
        
        public List<String> getNoticesList() {
            return noticesList;
        }
        
        @Override
        public void setNoticesList(List<String> noticesList) {
            this.noticesList = noticesList;
        }
    }
    
    /**
     * Constructor - defaults to multiple notice response
     */
    public NoticeResponse() {
        this.data = new MultipleNoticeResponseData();
        this.data.setAppCode("OCMS-2000"); // Default app code
    }
    
    /**
     * Get the response data
     */
    public ResponseData getData() {
        return data;
    }
    
    /**
     * Set the response data
     */
    public void setData(ResponseData data) {
        this.data = data;
    }
    
    /**
     * Create a single notice success response
     */
    public void createSingleNoticeSuccessResponse(String noticeNo) {
        SingleNoticeResponseData singleData = new SingleNoticeResponseData();
        singleData.setAppCode("OCMS-2000");
        singleData.setMessage("Your notice has been successfully created");
        singleData.setNoticeNo(noticeNo);
        this.data = singleData;
    }
    
    /**
     * Create a single notice failure response
     */
    public void createSingleNoticeFailureResponse(ErrorCodes errorCode, String errorDescription) {
        SingleNoticeResponseData singleData = new SingleNoticeResponseData();
        
        // Ensure we always have an error code
        if (errorCode == null) {
            errorCode = ErrorCodes.INTERNAL_SERVER_ERROR;
        }
        
        // Set the app code
        singleData.setAppCode(errorCode.getAppCode());
        
        // Ensure we always have an error message
        if (errorDescription == null || errorDescription.trim().isEmpty()) {
            singleData.setMessage(errorCode.getMessage());
        } else {
            singleData.setMessage(errorDescription);
        }
        
        this.data = singleData;
    }
    
    /**
     * Create a multiple notice all success response
     */
    public void createMultipleNoticeAllSuccessResponse(List<String> noticesList, String totalNotices) {
        MultipleNoticeResponseData multipleData = new MultipleNoticeResponseData();
        multipleData.setAppCode("OCMS-2000");
        multipleData.setMessage(totalNotices + " out of " + totalNotices + " case(s) have been successfully created");
        multipleData.setNoticesProcessed(totalNotices);
        multipleData.setNoticesFailed("0");
        multipleData.setFailedNotices(null);
        multipleData.setDuplicateNoticesCount(null);
        multipleData.setDuplicateNotices(null);
        multipleData.setTotalNotices(totalNotices);
        
        if (noticesList != null && !noticesList.isEmpty()) {
            // Set the notice list directly instead of joining them into a comma-separated string
            multipleData.setNoticesList(noticesList);
        }
        
        this.data = multipleData;
    }
    
    /**
     * Create a multiple notice partial success response (some succeeded, some failed)
     */
    public void createMultipleNoticePartialSuccessResponse(List<String> successNotices, 
                                                          String failedNoticesDescription,
                                                          String duplicateNoticesCount,
                                                          String duplicateNotices,
                                                          String totalNotices) {
        MultipleNoticeResponseData multipleData = new MultipleNoticeResponseData();
        multipleData.setAppCode("OCMS-2006");
        
        // Calculate failed notices count (total - success)
        int totalCount = Integer.parseInt(totalNotices);
        int successCount = successNotices.size();
        int failedCount = totalCount - successCount;
        int duplicateCount = (duplicateNoticesCount != null) ? Integer.parseInt(duplicateNoticesCount) : 0;
        int regularFailedCount = failedCount - duplicateCount;
        
        // Create appropriate message based on failure and duplicate counts
        String message;
        if (duplicateCount > 0 && regularFailedCount > 0) {
            message = duplicateCount + " duplicate case(s) & " + regularFailedCount + " case(s) found failed to create notices";
        } else if (duplicateCount > 0) {
            message = duplicateCount + " duplicate case(s) out of " + totalNotices + " case(s) failed to create notices";
        } else {
            message = failedCount + " out of " + totalNotices + " case(s) failed to create notices";
        }
        
        multipleData.setMessage(message);
        multipleData.setNoticesProcessed(String.valueOf(successNotices.size()));
        multipleData.setNoticesFailed(String.valueOf(failedCount));
        multipleData.setFailedNotices(failedNoticesDescription);
        multipleData.setDuplicateNoticesCount(duplicateNoticesCount);
        multipleData.setDuplicateNotices(duplicateNotices);
        multipleData.setTotalNotices(totalNotices);
        
        if (successNotices != null && !successNotices.isEmpty()) {
            // Set the notice list directly instead of joining them into a comma-separated string
            multipleData.setNoticesList(successNotices);
        } else {
            multipleData.setNoticesList(new ArrayList<>());
        }
        
        this.data = multipleData;
    }

    /**
     * Create a multiple notice all failed response
     */
    public void createMultipleNoticeAllFailedResponse(ErrorCodes errorCode, 
                                                    String errorDescription,
                                                    String totalNoticesProcessed,
                                                    String totalFailedNotices,
                                                    String failedNoticesDescription,
                                                    String duplicateNoticesCount,
                                                    String duplicateNotices,
                                                    String totalNotices) {
        MultipleNoticeResponseData multipleData = new MultipleNoticeResponseData();
        multipleData.setAppCode("OCMS-4000"); // Explicitly set to OCMS-4000
        
        // Create appropriate message based on duplicate count
        String message;
        int duplicateCount = (duplicateNoticesCount != null) ? Integer.parseInt(duplicateNoticesCount) : 0;
        int regularFailedCount = Integer.parseInt(totalFailedNotices) - duplicateCount;
        
        if (duplicateCount > 0 && regularFailedCount > 0) {
            message = duplicateCount + " duplicate case(s) & " + regularFailedCount + " case(s) found failed to create notices";
        } else if (duplicateCount > 0) {
            message = duplicateCount + " duplicate case(s) out of " + totalNotices + " case(s) failed to create notices";
        } else {
            message = totalFailedNotices + " out of " + totalNotices + " case(s) failed to create notices";
        }
        
        multipleData.setMessage(message);
        multipleData.setNoticesProcessed("0"); // Always 0 for all failed
        multipleData.setNoticesFailed(totalFailedNotices);
        multipleData.setFailedNotices(failedNoticesDescription);
        multipleData.setDuplicateNoticesCount(duplicateNoticesCount);
        multipleData.setDuplicateNotices(duplicateNotices);
        multipleData.setTotalNotices(totalNotices);
        multipleData.setNoticesList(new ArrayList<>()); // Empty list for all failed

        this.data = multipleData;
    }
    
    /**
     * Create a single notice response
     * @deprecated Use createSingleNoticeSuccessResponse or createSingleNoticeFailureResponse instead
     */
    @Deprecated
    public void createSingleNoticeResponse(String noticeNo) {
        createSingleNoticeSuccessResponse(noticeNo);
    }
    
    /**
     * Create a multiple notice response
     * @deprecated Use specific multiple notice response methods instead
     */
    @Deprecated
    public void createMultipleNoticeResponse(List<String> noticesList) {
        MultipleNoticeResponseData multipleData = new MultipleNoticeResponseData();
        if (this.data != null) {
            multipleData.setAppCode(this.data.getAppCode());
            multipleData.setMessage(this.data.getMessage());
        }
        if (noticesList != null) {
            multipleData.setNoticesList(noticesList);
        }
        this.data = multipleData;
    }
    
    // Helper methods for backward compatibility
    public void setHTTPStatusCode(String code) {
        // Not used in new format
        log.debug("HTTPStatusCode is not used in new response format: {}", code);
    }
    
    public void setHTTPStatusDescription(String description) {
        // Not used in new format
        log.debug("HTTPStatusDescription is not used in new response format: {}", description);
    }
    
    public void setMessage(String message) {
        if (this.data != null) {
            this.data.setMessage(message);
        }
    }
    
    public void setNoticesProcessed(String noticesProcessed) {
        if (this.data != null) {
            this.data.setNoticesProcessed(noticesProcessed);
        }
    }
    
    public void setSuccessfulNoticeNumbers(String successfulNoticeNumbers) {
        if (successfulNoticeNumbers != null) {
            if (successfulNoticeNumbers.contains(",")) {
                // Multiple notices
                List<String> noticesList = List.of(successfulNoticeNumbers.split(","));
                createMultipleNoticeAllSuccessResponse(noticesList, String.valueOf(noticesList.size()));
            } else {
                // Single notice
                createSingleNoticeSuccessResponse(successfulNoticeNumbers);
            }
        }
    }
    
    public void setData(List<String> noticesList) {
        if (noticesList != null) {
            if (noticesList.size() == 1) {
                // Single notice
                createSingleNoticeSuccessResponse(noticesList.get(0));
            } else {
                // Multiple notices
                createMultipleNoticeAllSuccessResponse(noticesList, String.valueOf(noticesList.size()));
            }
        }
    }
    
    public void setNoticesFailed(String noticesFailed) {
        if (this.data != null) {
            this.data.setNoticesFailed(noticesFailed);
        }
    }
    
    public void setFailedNotices(String failedNotices) {
        if (this.data != null) {
            this.data.setFailedNotices(failedNotices);
        }
    }
    
    public void setDuplicateNoticesCount(String duplicateNoticesCount) {
        if (this.data != null) {
            this.data.setDuplicateNoticesCount(duplicateNoticesCount);
        }
    }
    
    public void setDuplicateNotices(String duplicateNotices) {
        if (this.data != null) {
            this.data.setDuplicateNotices(duplicateNotices);
        }
    }
    
    public void setTotalNotices(String totalNotices) {
        if (this.data != null) {
            this.data.setTotalNotices(totalNotices);
        }
    }
}

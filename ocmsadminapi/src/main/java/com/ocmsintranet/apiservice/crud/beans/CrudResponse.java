package com.ocmsintranet.apiservice.crud.beans;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified response class for all CRUD operations
 * 
 * @param <T> Type of data being returned
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrudResponse<T> {
    // Standard response structure
    private ResponseData data;
    
    /**
     * Response data container
     * JsonInclude.Include.NON_NULL ensures null fields are not included in the JSON output
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ResponseData<T> {
        // Standard fields for all responses
        private String appCode;
        private String message;
        
        // Optional data fields
        private T result;
        private List<T> items;
        private Long total;
        private Integer limit;
        private Integer skip;
    }
    
    /**
     * Error and success codes for CRUD operations
     */
    public static class AppCodes {
        // Success codes (2000-2999)
        public static final String SUCCESS = "OCMS-2000";
        public static final String CREATED = "OCMS-2001";
        public static final String UPDATED = "OCMS-2002";
        public static final String DELETED = "OCMS-2003";
        
        // Client error codes (4000-4999)
        public static final String BAD_REQUEST = "OCMS-4000";
        public static final String UNAUTHORIZED = "OCMS-4001";
        public static final String FORBIDDEN = "OCMS-4003";
        public static final String NOT_FOUND = "OCMS-4004";
        public static final String INVALID_INPUT = "OCMS-4005";
        public static final String DUPLICATE_RECORD = "OCMS-4006";
        public static final String NON_EDITABLE_FIELD = "OCMS-4007";
        public static final String CONFLICT = "OCMS-4009";
        
        // Server error codes (5000-5999)
        public static final String INTERNAL_SERVER_ERROR = "OCMS-5000";
        public static final String DATABASE_CONNECTION_FAILED = "OCMS-5001";
        public static final String REQUEST_TIMEOUT = "OCMS-5002";
        public static final String DATABASE_QUERY_ERROR = "OCMS-5003";
        public static final String SERVICE_UNAVAILABLE = "OCMS-5004";
    }
    
    /**
     * Standard messages for common operations
     */
    public static class Messages {
        public static final String SAVE_SUCCESS = "Save Success.";
        public static final String UPDATE_SUCCESS = "Update Success.";
        public static final String DELETE_SUCCESS = "Delete Success.";
        public static final String FETCH_SUCCESS = "Fetch Success.";
        public static final String RECORD_NOT_FOUND = "The requested record could not be found.";
        public static final String DATABASE_ERROR = "Something went wrong. Please try again later.";
        public static final String INVALID_INPUT = "The request could not be processed due to invalid input data.";
        public static final String DUPLICATE_RECORD = "Data already exists.";
        public static final String NON_EDITABLE_FIELD = "Modification of non-editable field is not allowed.";
    }
    
    /**
     * Create a success response with a message
     */
    public static <T> CrudResponse<T> success(String message) {
        ResponseData<T> responseData = new ResponseData<>();
        responseData.setAppCode(AppCodes.SUCCESS);
        responseData.setMessage(message);
        
        CrudResponse<T> response = new CrudResponse<>();
        response.setData(responseData);
        return response;
    }
    
    /**
     * Create a success response with entity data
     */
    public static <T> CrudResponse<T> success(String message, T entity) {
        ResponseData<T> responseData = new ResponseData<>();
        responseData.setAppCode(AppCodes.SUCCESS);
        responseData.setMessage(message);
        responseData.setResult(entity);
        
        CrudResponse<T> response = new CrudResponse<>();
        response.setData(responseData);
        return response;
    }
    
    /**
     * Create a success response with paginated list data
     */
    public static <T> CrudResponse<T> successList(String message, List<T> items, long total, int limit, int skip) {
        ResponseData<T> responseData = new ResponseData<>();
        responseData.setAppCode(AppCodes.SUCCESS);
        responseData.setMessage(message);
        responseData.setItems(items);
        responseData.setTotal(total);
        responseData.setLimit(limit);
        responseData.setSkip(skip);
        
        CrudResponse<T> response = new CrudResponse<>();
        response.setData(responseData);
        return response;
    }
    
    /**
     * Create an error response
     */
    public static <T> CrudResponse<T> error(String appCode, String message) {
        ResponseData<T> responseData = new ResponseData<>();
        responseData.setAppCode(appCode);
        responseData.setMessage(message);
        
        CrudResponse<T> response = new CrudResponse<>();
        response.setData(responseData);
        return response;
    }
}
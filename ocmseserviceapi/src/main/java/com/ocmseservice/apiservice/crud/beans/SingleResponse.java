package com.ocmseservice.apiservice.crud.beans;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Generic response class for single object responses
 * Example usage:
 * {
 *     "data": {
 *         "appCode": "OCMS-2000",
 *         "message": "Error processing request: Database operation failed"
 *     }
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SingleResponse<T> implements Serializable {


    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long total;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer limit;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer skip;
    private T data;
    


    /**
     * Private constructor for use by helper methods.
     * @param data The data payload.
     */
    private SingleResponse(T data) {
        this.data = data;
    }
    
    /**
     * NEW - Helper method to create a response with paginated data.
     * @param items The list of items for the current page.
     * @param total The total number of items available.
     * @param limit The number of items per page.
     * @param skip The number of items skipped.
     * @return SingleResponse with the paginated list and metadata.
     */
    public static <E> SingleResponse<List<E>> createPaginatedResponse(List<E> items, long total, int limit, int skip) {
        SingleResponse<List<E>> response = new SingleResponse<>();
        response.setData(items);
        response.setTotal(total);
        response.setLimit(limit);
        response.setSkip(skip);
        return response;
    }
    /**
     * Helper method to create a response with an error message
     * @param appCode The application error code
     * @param message The error message
     * @return SingleResponse with ErrorData
     */
    public static SingleResponse<ErrorData> createErrorResponse(String appCode, String message) {
        ErrorData errorData = new ErrorData(appCode, message);
        return new SingleResponse<>(errorData);
    }
    
    /**
     * Helper method to create a response with a status message
     * @param status The status message
     * @return SingleResponse with StatusData
     */
    public static SingleResponse<StatusData> createStatusResponse(String status) {
        StatusData statusData = new StatusData(status);
        return new SingleResponse<>(statusData);
    }
    
    /**
     * Inner class for error responses
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorData implements Serializable {
        private String appCode;
        private String message;

    }
    
    /**
     * Inner class for status responses
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusData implements Serializable {
        private String status;
    }
}

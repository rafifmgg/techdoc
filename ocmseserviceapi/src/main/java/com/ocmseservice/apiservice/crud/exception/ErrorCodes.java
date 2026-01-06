package com.ocmseservice.apiservice.crud.exception;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public enum ErrorCodes {
    // Redirection Errors (3000-3999)
    MULTIPLE_CHOICES("OCMS-3000", "There are multiple resources available. Please choose the appropriate one."),
    MOVED_PERMANENTLY("OCMS-3001", "This resource has been permanently moved to a new location."),
    TEMPORARY_REDIRECT("OCMS-3002", "This resource is temporarily moved. Please update your bookmarks."),
    SEE_OTHER("OCMS-3003", "The resource is available at another location. Redirecting..."),
    NOT_MODIFIED("OCMS-3004", "The resource has not changed since your last visit."),
    USE_PROXY("OCMS-3005", "This resource must be accessed via a proxy server."),
    UNUSED("OCMS-3006", "This status code is not in use."),
    TEMPORARY_REDIRECT_SAME_METHOD("OCMS-3007", "This resource is temporarily moved. Please use the same request method."),
    PERMANENT_REDIRECT("OCMS-3008", "This resource has permanently moved, and the request method remains unchanged."),

    // Client Errors (4000-4999)
    BAD_REQUEST("OCMS-4000", "The request could not be processed due to a syntax error. Please check and try again."),
    UNAUTHORIZED("OCMS-4001", "You are not authorized to access this resource. Please log in and try again."),
    PAYMENT_REQUIRED("OCMS-4002", "This resource requires payment to access. Please complete the payment and try again."),
    FORBIDDEN("OCMS-4003", "You do not have permission to access this resource."),
    NOT_FOUND("OCMS-4004", "The page or resource you are looking for could not be found."),

    // System Errors (5000-5999)
    INTERNAL_SERVER_ERROR("OCMS-5000", "Something went wrong. Please try again later"),
    DATABASE_CONNECTION_FAILED("OCMS-5001", "Unable to save data due to server issues. Please try again later"),
    REQUEST_TIMEOUT("OCMS-5002", "The request timed out. Please try again later"),
    UNEXPECTED_ERROR("OCMS-5003", "Something went wrong. Please try again later"),
    SERVICE_UNAVAILABLE("OCMS-5004", "The system is currently experiencing service issues. Please try again later"),
    AUDIT_LOG_FAILED("OCMS-5005", "Unable to log the update action. Please try again later"),
    DATABASE_RETRIEVAL_FAILED("OCMS-5006", "The system is experiencing issues. Please try again later");

    private final String appCode;
    private final String message;
}

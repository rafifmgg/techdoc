package com.ocmsintranet.apiservice.crud.exception;

import com.ocmsintranet.apiservice.crud.beans.ErrorMessage;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.PersistenceException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.http.ResponseEntity;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorMessage> handleResponseStatusException(ResponseStatusException ex) {
        // For UNAUTHORIZED status, use the UNAUTHORIZED error code
        if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            ErrorCodes error = ErrorCodes.UNAUTHORIZED;
            return buildErrorResponse(error, error.getMessage(), HttpStatus.UNAUTHORIZED.value());
        }
        
        // For other statuses, keep existing logic
        ErrorCodes error = ErrorCodes.BAD_REQUEST;
        return buildErrorResponse(error, ex.getReason(), ex.getStatusCode().value());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessage> handleValidationException(MethodArgumentNotValidException ex) {
        ErrorCodes error = ErrorCodes.BAD_REQUEST;
        return buildErrorResponse(error, ex.getMessage(), HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorMessage> handleInvalidFormatException(HttpMessageNotReadableException ex) {
        ErrorCodes error = ErrorCodes.BAD_REQUEST;
        return buildErrorResponse(error, ex.getMessage(), HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(PersistenceException.class)
    public ResponseEntity<ErrorMessage> handlePersistenceException(PersistenceException ex) {
        ErrorCodes error = ErrorCodes.DATABASE_CONNECTION_FAILED;
        return buildErrorResponse(error, ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorMessage> handleDataAccessException(DataAccessException ex) {
        ErrorCodes error = ErrorCodes.DATABASE_CONNECTION_FAILED;
        return buildErrorResponse(error, ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ErrorMessage> handleTransactionException(TransactionSystemException ex) {
        ErrorCodes error = ErrorCodes.DATABASE_CONNECTION_FAILED;
        return buildErrorResponse(error, ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<ErrorMessage> handleTimeoutException(AsyncRequestTimeoutException ex) {
        ErrorCodes error = ErrorCodes.REQUEST_TIMEOUT;
        return buildErrorResponse(error, ex.getMessage(), HttpStatus.REQUEST_TIMEOUT.value());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleGenericException(Exception ex) {
        ErrorCodes error = ErrorCodes.UNEXPECTED_ERROR;
        return buildErrorResponse(error, error.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    private ResponseEntity<ErrorMessage> buildErrorResponse(ErrorCodes errorCodes, String detailMessage, int httpStatus) {
        ErrorMessage errorMessage = new ErrorMessage(
                new ErrorMessage.ErrorData(errorCodes.getAppCode(), detailMessage != null ? detailMessage : errorCodes.getMessage())
        );
        return ResponseEntity.status(httpStatus).body(errorMessage);
    }
}

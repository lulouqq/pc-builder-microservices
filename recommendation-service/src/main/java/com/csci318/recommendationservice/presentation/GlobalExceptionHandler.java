package com.csci318.recommendationservice.presentation;

import com.csci318.recommendationservice.domain.ValidationException;
import com.csci318.recommendationservice.service.AuthenticationRequiredException;
import com.csci318.recommendationservice.service.NoValidBuildException;
import com.csci318.recommendationservice.service.ResourceNotFoundException;
import com.csci318.recommendationservice.service.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps exceptions to the API error response contract.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(ValidationException ex) {
        return new ErrorResponse("ValidationFailed", ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnreadableBody(HttpMessageNotReadableException ex) {
        return new ErrorResponse("ValidationFailed", "Request body is missing or malformed");
    }

    /** 401 when Account Service rejected the forwarded token (R1). */
    @ExceptionHandler(AuthenticationRequiredException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationRequired(AuthenticationRequiredException ex) {
        return new ErrorResponse("AuthenticationRequired", ex.getMessage());
    }

    /** 403 for an owner check made in the service (R1 body userId, R3 stored userId) or by Account Service. */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDenied(AccessDeniedException ex) {
        return new ErrorResponse("AccessDenied", ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException ex) {
        return new ErrorResponse("ResourceNotFound", ex.getMessage());
    }

    @ExceptionHandler(NoValidBuildException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleNoValidBuild(NoValidBuildException ex) {
        return new ErrorResponse("CompatibilityConflict", ex.getMessage());
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleServiceUnavailable(ServiceUnavailableException ex) {
        return new ErrorResponse("ServiceUnavailable", ex.getMessage());
    }

    /**
     * Any other failure is an InternalServiceError (500); details are logged, not returned.
     * Standard Spring MVC errors (e.g. unknown path 404, 405, 415) are rethrown so Spring keeps their status,
     * and Spring Security authentication exceptions are rethrown so Spring Security answers with 401.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception ex) throws Exception {
        if (ex instanceof org.springframework.web.ErrorResponse || ex instanceof AuthenticationException) {
            throw ex;
        }
        log.error("Unexpected error", ex);
        return new ErrorResponse("InternalServiceError", "An unexpected error occurred");
    }
}

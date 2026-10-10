package com.csci318.catalogservice.presentation;

import com.csci318.catalogservice.presentation.dto.ErrorResponse;
import com.csci318.catalogservice.service.CategoryInUseException;
import com.csci318.catalogservice.service.ResourceNotFoundException;
import com.csci318.catalogservice.service.ValidationFailedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/**
 * Maps exceptions to the error codes of the API error contract.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ValidationFailedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationFailed(ValidationFailedException e) {
        return new ErrorResponse("ValidationFailed", e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException e) {
        return new ErrorResponse("ResourceNotFound", e.getMessage());
    }

    @ExceptionHandler(CategoryInUseException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleCategoryInUse(CategoryInUseException e) {
        return new ErrorResponse("CategoryInUse", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception e) {
        logger.error("Unexpected error", e);
        return new ErrorResponse("InternalServiceError", "Unexpected service-side failure");
    }

    /**
     * Standard Spring MVC errors (invalid or unreadable body, unknown path, ...) are handled by the
     * superclass; here their body is replaced with the contract's error code where one applies.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.value() == HttpStatus.BAD_REQUEST.value()) {
            body = new ErrorResponse("ValidationFailed", validationMessage(ex));
        } else if (statusCode.value() == HttpStatus.NOT_FOUND.value()) {
            body = new ErrorResponse("ResourceNotFound", "Resource not found");
        } else if (statusCode.value() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            body = new ErrorResponse("InternalServiceError", "Unexpected service-side failure");
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private String validationMessage(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException invalid) {
            return invalid.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + " " + error.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        }
        return "Request is missing or malformed";
    }
}

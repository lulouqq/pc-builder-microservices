package com.csci318.accountservice.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Writes Spring Security's 401 and 403 responses in the API error response contract format.
 */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public SecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 401: no token, or a malformed, expired or wrongly signed one. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        String message = ex instanceof OAuth2AuthenticationException
                ? "Access token is invalid or expired"
                : "Authentication is required";
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(response, HttpServletResponse.SC_UNAUTHORIZED, new ErrorResponse("AuthenticationRequired", message));
    }

    /** 403: valid token, but not for the requested user. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        write(response, HttpServletResponse.SC_FORBIDDEN,
                new ErrorResponse("AccessDenied", "You do not have permission to access this resource"));
    }

    private void write(HttpServletResponse response, int status, ErrorResponse body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

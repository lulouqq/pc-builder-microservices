package com.csci318.accountservice.presentation;

import com.csci318.accountservice.service.AuthenticationResult;

import java.time.Instant;

/**
 * Response of POST /auth/login. The token is sent on later requests as "Authorization: Bearer &lt;token&gt;".
 */
public record AuthenticationResponse(String token, String tokenType, Instant expiresAt, String userId, String role) {

    public static AuthenticationResponse from(AuthenticationResult result) {
        return new AuthenticationResponse(result.token(), "Bearer", result.expiresAt(), result.userId(),
                result.role().name());
    }
}

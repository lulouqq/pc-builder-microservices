package com.csci318.accountservice.service;

import com.csci318.accountservice.domain.Role;

import java.time.Instant;

public record AuthenticationResult(String token, Instant expiresAt, String userId, Role role) {
}

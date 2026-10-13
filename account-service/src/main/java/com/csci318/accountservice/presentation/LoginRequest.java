package com.csci318.accountservice.presentation;

/**
 * Request body for POST /auth/login.
 */
public record LoginRequest(String username, String password) {
}

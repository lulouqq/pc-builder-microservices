package com.csci318.accountservice.service;

/**
 * Input for registering a new user (A1). The password is plain text here and is hashed by {@link UserService}.
 */
public record RegisterUserCommand(
        String username,
        String email,
        String phone,
        String password,
        String firstName,
        String lastName) {
}

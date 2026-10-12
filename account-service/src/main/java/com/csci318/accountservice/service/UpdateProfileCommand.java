package com.csci318.accountservice.service;

/**
 * Input for replacing a user's profile (A1). Both fields are optional; a missing field clears it.
 */
public record UpdateProfileCommand(String firstName, String lastName) {
}

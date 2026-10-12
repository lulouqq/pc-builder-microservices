package com.csci318.accountservice.domain;

import java.time.Instant;

/**
 * Domain event raised when a user's profile actually changes (A1).
 * Internal only: published in-process via Spring, not to Kafka.
 */
public record UserProfileUpdated(String userId, String firstName, String lastName, Instant occurredAt) {
}

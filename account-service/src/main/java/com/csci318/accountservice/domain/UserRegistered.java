package com.csci318.accountservice.domain;

import java.time.Instant;

/**
 * Domain event raised when a new user registers (A1).
 * Internal only: published in-process via Spring, not to Kafka.
 */
public record UserRegistered(String userId, String username, Instant occurredAt) {
}

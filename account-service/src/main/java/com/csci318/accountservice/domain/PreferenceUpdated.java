package com.csci318.accountservice.domain;

import java.time.Instant;

/**
 * Domain event raised when a user's saved preference actually changes (A3).
 * Internal only: published in-process via Spring, not to Kafka.
 */
public record PreferenceUpdated(String userId, String preference, Instant occurredAt) {
}

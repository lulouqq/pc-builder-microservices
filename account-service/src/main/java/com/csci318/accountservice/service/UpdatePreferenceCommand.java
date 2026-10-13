package com.csci318.accountservice.service;

/**
 * Input for replacing a user's saved preference (A3). A missing, null or blank preference clears it.
 */
public record UpdatePreferenceCommand(String preference) {
}

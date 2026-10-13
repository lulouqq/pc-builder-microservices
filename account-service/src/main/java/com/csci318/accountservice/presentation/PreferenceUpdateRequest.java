package com.csci318.accountservice.presentation;

import com.csci318.accountservice.service.UpdatePreferenceCommand;

/**
 * Request body for PUT /users/{userId}/preferences. Replaces the saved preference; a missing, null or blank
 * preference clears it.
 */
public record PreferenceUpdateRequest(String preference) {

    public UpdatePreferenceCommand toCommand() {
        return new UpdatePreferenceCommand(preference);
    }
}

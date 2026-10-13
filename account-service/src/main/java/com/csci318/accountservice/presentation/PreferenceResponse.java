package com.csci318.accountservice.presentation;

import com.csci318.accountservice.domain.User;

/**
 * A user's saved recommendation preference; preference is null when none is saved.
 */
public record PreferenceResponse(String userId, String preference) {

    public static PreferenceResponse from(User user) {
        return new PreferenceResponse(user.getId(), user.getPreference().preference());
    }
}

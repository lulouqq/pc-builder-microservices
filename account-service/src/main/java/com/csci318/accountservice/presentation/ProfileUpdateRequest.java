package com.csci318.accountservice.presentation;

import com.csci318.accountservice.service.UpdateProfileCommand;

/**
 * Request body for PUT /users/{userId}/profile. Replaces the whole profile; both fields are optional.
 */
public record ProfileUpdateRequest(String firstName, String lastName) {

    public UpdateProfileCommand toCommand() {
        return new UpdateProfileCommand(firstName, lastName);
    }
}

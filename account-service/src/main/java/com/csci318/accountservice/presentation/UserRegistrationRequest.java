package com.csci318.accountservice.presentation;

import com.csci318.accountservice.service.RegisterUserCommand;

/**
 * Request body for POST /users. firstName and lastName (UserProfile) are optional.
 */
public record UserRegistrationRequest(
        String username,
        String email,
        String phone,
        String password,
        String firstName,
        String lastName) {

    public RegisterUserCommand toCommand() {
        return new RegisterUserCommand(username, email, phone, password, firstName, lastName);
    }
}

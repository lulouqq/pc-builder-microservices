package com.csci318.accountservice.presentation;

import com.csci318.accountservice.domain.User;
import com.csci318.accountservice.domain.UserProfile;

/**
 * Account and profile information returned to clients. The password hash is never exposed.
 */
public record UserResponse(
        String id,
        String username,
        String email,
        String phone,
        String firstName,
        String lastName) {

    public static UserResponse from(User user) {
        UserProfile profile = user.getProfile();
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                profile != null ? profile.firstName() : null,
                profile != null ? profile.lastName() : null);
    }
}

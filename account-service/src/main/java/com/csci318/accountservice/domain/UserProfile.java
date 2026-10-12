package com.csci318.accountservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Value object: personal profile details of a user (both fields optional).
 */
@Embeddable
public record UserProfile(
        @Column(name = "first_name") String firstName,
        @Column(name = "last_name") String lastName) {
}

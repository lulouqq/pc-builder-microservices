package com.csci318.accountservice.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void registerCreatesUserWithIdAndProfile() {
        User user = User.register("alice", "alice@example.com", "0400000000", "hash",
                new UserProfile("Alice", "Smith"));

        assertThat(user.getId()).isNotBlank();
        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getPhone()).isEqualTo("0400000000");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.getProfile()).isEqualTo(new UserProfile("Alice", "Smith"));
    }

    @Test
    void registerRejectsMissingRequiredFields() {
        assertThatThrownBy(() -> User.register(" ", "a@example.com", "0400000000", "hash", null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("username");
        assertThatThrownBy(() -> User.register("alice", null, "0400000000", "hash", null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("email");
        assertThatThrownBy(() -> User.register("alice", "a@example.com", "", "hash", null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("phone");
        assertThatThrownBy(() -> User.register("alice", "a@example.com", "0400000000", null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("passwordHash");
    }
}

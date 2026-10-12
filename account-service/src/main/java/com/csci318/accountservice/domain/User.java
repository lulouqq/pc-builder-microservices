package com.csci318.accountservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root: a registered customer account with its authentication information.
 * Domain events registered here are published when the aggregate is saved through its repository.
 */
@Entity
@Table(name = "users")
public class User extends AbstractAggregateRoot<User> {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String passwordHash;

    @Embedded
    private UserProfile profile;

    protected User() {
        // for JPA
    }

    private User(String id, String username, String email, String phone, String passwordHash, UserProfile profile) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.profile = profile;
    }

    /**
     * Registers a new user. The password must already be hashed; the plain password is never stored.
     */
    public static User register(String username, String email, String phone, String passwordHash, UserProfile profile) {
        requireText(username, "username");
        requireText(email, "email");
        requireText(phone, "phone");
        requireText(passwordHash, "passwordHash");

        User user = new User(UUID.randomUUID().toString(), username, email, phone, passwordHash, profile);
        user.registerEvent(new UserRegistered(user.id, user.username, Instant.now()));
        return user;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required");
        }
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserProfile getProfile() {
        return profile;
    }
}

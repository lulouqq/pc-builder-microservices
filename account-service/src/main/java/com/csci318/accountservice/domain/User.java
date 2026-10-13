package com.csci318.accountservice.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Embedded
    private UserProfile profile;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private List<Address> addresses = new ArrayList<>();

    protected User() {
        // for JPA
    }

    private User(String id, String username, String email, String phone, String passwordHash, Role role,
                 UserProfile profile) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.profile = profile;
    }

    /**
     * Registers a new customer. The password must already be hashed; the plain password is never stored.
     */
    public static User register(String username, String email, String phone, String passwordHash, UserProfile profile) {
        return create(username, email, phone, passwordHash, Role.CUSTOMER, profile);
    }

    /**
     * Creates the administrator account (seeded from configuration at startup, never through the API).
     */
    public static User registerAdmin(String username, String email, String phone, String passwordHash) {
        return create(username, email, phone, passwordHash, Role.ADMIN, null);
    }

    private static User create(String username, String email, String phone, String passwordHash, Role role,
                               UserProfile profile) {
        requireText(username, "username");
        requireText(email, "email");
        requireText(phone, "phone");
        requireText(passwordHash, "passwordHash");

        User user = new User(UUID.randomUUID().toString(), username, email, phone, passwordHash, role, profile);
        user.registerEvent(new UserRegistered(user.id, user.username, Instant.now()));
        return user;
    }

    /**
     * Replaces the profile. UserProfileUpdated is raised only when the profile actually changes.
     */
    public void updateProfile(UserProfile newProfile) {
        UserProfile updated = newProfile != null ? newProfile : new UserProfile(null, null);
        if (updated.equals(getProfileOrEmpty())) {
            return;
        }
        this.profile = updated;
        registerEvent(new UserProfileUpdated(id, updated.firstName(), updated.lastName(), Instant.now()));
    }

    public Address addAddress(Integer streetNo, String streetName, String suburb, String city, String state,
                              Integer postCode) {
        Address address = new Address(streetNo, streetName, suburb, city, state, postCode);
        addresses.add(address);
        return address;
    }

    /** Returns the updated address, or empty if this user has no address with the given id. */
    public Optional<Address> updateAddress(String addressId, Integer streetNo, String streetName, String suburb,
                                           String city, String state, Integer postCode) {
        Optional<Address> address = findAddress(addressId);
        address.ifPresent(a -> a.update(streetNo, streetName, suburb, city, state, postCode));
        return address;
    }

    /** Returns false if this user has no address with the given id. */
    public boolean removeAddress(String addressId) {
        return addresses.removeIf(a -> a.getId().equals(addressId));
    }

    public Optional<Address> findAddress(String addressId) {
        return addresses.stream().filter(a -> a.getId().equals(addressId)).findFirst();
    }

    // Hibernate loads an embedded profile whose columns are all null as null.
    private UserProfile getProfileOrEmpty() {
        return Objects.requireNonNullElseGet(profile, () -> new UserProfile(null, null));
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

    public Role getRole() {
        return role;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public List<Address> getAddresses() {
        return Collections.unmodifiableList(addresses);
    }
}

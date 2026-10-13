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

    @Test
    void updateProfileReplacesProfile() {
        User user = User.register("alice", "alice@example.com", "0400000000", "hash",
                new UserProfile("Alice", "Smith"));

        user.updateProfile(new UserProfile("Alicia", null));

        assertThat(user.getProfile()).isEqualTo(new UserProfile("Alicia", null));
    }

    @Test
    void addUpdateAndRemoveAddress() {
        User user = User.register("alice", "alice@example.com", "0400000000", "hash", null);

        Address address = user.addAddress(1, "Main St", "Wollongong", "Wollongong", "NSW", 2500);
        assertThat(address.getId()).isNotBlank();
        assertThat(user.getAddresses()).containsExactly(address);

        assertThat(user.updateAddress(address.getId(), 2, "High St", "Fairy Meadow", "Wollongong", "NSW", 2519))
                .hasValueSatisfying(updated -> {
                    assertThat(updated.getStreetNo()).isEqualTo(2);
                    assertThat(updated.getStreetName()).isEqualTo("High St");
                    assertThat(updated.getPostCode()).isEqualTo(2519);
                });
        assertThat(user.updateAddress("unknown", 2, "High St", "Fairy Meadow", "Wollongong", "NSW", 2519))
                .isEmpty();

        assertThat(user.removeAddress("unknown")).isFalse();
        assertThat(user.removeAddress(address.getId())).isTrue();
        assertThat(user.getAddresses()).isEmpty();
    }

    @Test
    void addressRejectsMissingRequiredFields() {
        User user = User.register("alice", "alice@example.com", "0400000000", "hash", null);

        assertThatThrownBy(() -> user.addAddress(null, "Main St", "Wollongong", "Wollongong", "NSW", 2500))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("streetNo");
        assertThatThrownBy(() -> user.addAddress(1, " ", "Wollongong", "Wollongong", "NSW", 2500))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("streetName");
        assertThatThrownBy(() -> user.addAddress(1, "Main St", "Wollongong", "Wollongong", "NSW", null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("postCode");
        assertThat(user.getAddresses()).isEmpty();
    }

    @Test
    void failedAddressUpdateLeavesAddressUnchanged() {
        User user = User.register("alice", "alice@example.com", "0400000000", "hash", null);
        Address address = user.addAddress(1, "Main St", "Wollongong", "Wollongong", "NSW", 2500);

        assertThatThrownBy(() -> user.updateAddress(address.getId(), 2, "High St", "", "Wollongong", "NSW", 2519))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("suburb");
        assertThat(address.getStreetNo()).isEqualTo(1);
        assertThat(address.getSuburb()).isEqualTo("Wollongong");
    }

    @Test
    void registeredUserIsCustomer() {
        User user = User.register("alice", "alice@example.com", "0400000000", "hash", null);

        assertThat(user.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void registerAdminCreatesAdmin() {
        User admin = User.registerAdmin("admin", "admin@example.com", "0400000000", "hash");

        assertThat(admin.getId()).isNotBlank();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getProfile()).isNull();
    }

    @Test
    void updatePreferenceReplacesAndClearsPreference() {
        User user = User.register("alice", "alice@example.com", "0400000000", "hash", null);
        assertThat(user.getPreference().preference()).isNull();

        user.updatePreference(new Preference("Quiet gaming PC"));
        assertThat(user.getPreference().preference()).isEqualTo("Quiet gaming PC");

        user.updatePreference(new Preference("Small form factor"));
        assertThat(user.getPreference().preference()).isEqualTo("Small form factor");

        user.updatePreference(new Preference("   "));
        assertThat(user.getPreference().preference()).isNull();

        user.updatePreference(new Preference("Quiet gaming PC"));
        user.updatePreference(null);
        assertThat(user.getPreference().preference()).isNull();
    }

    @Test
    void blankPreferenceEqualsNoPreference() {
        assertThat(new Preference(" ")).isEqualTo(new Preference(null));
        assertThat(new Preference("")).isEqualTo(new Preference(null));
    }
}

package com.csci318.accountservice.service;

import com.csci318.accountservice.domain.Address;
import com.csci318.accountservice.domain.User;
import com.csci318.accountservice.domain.UserProfile;
import com.csci318.accountservice.domain.ValidationException;
import com.csci318.accountservice.infrastructure.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(RegisterUserCommand command) {
        if (command.password() == null || command.password().isBlank()) {
            throw new ValidationException("password is required");
        }
        if (command.username() != null && userRepository.existsByUsername(command.username())) {
            throw new ValidationException("username is already taken");
        }

        User user = User.register(
                command.username(),
                command.email(),
                command.phone(),
                passwordEncoder.encode(command.password()),
                new UserProfile(command.firstName(), command.lastName()));
        try {
            // Flush now so a concurrent registration with the same username fails here, on the unique constraint.
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // username is the only unique column; required fields are already checked by User.register.
            throw new ValidationException("username is already taken");
        }
    }

    @Transactional(readOnly = true)
    public User getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    @Transactional
    public User updateProfile(String userId, UpdateProfileCommand command) {
        User user = getUser(userId);
        user.updateProfile(new UserProfile(command.firstName(), command.lastName()));
        // save publishes UserProfileUpdated (if the profile changed); dirty checking alone would not.
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<Address> getAddresses(String userId) {
        return List.copyOf(getUser(userId).getAddresses());
    }

    @Transactional
    public Address addAddress(String userId, AddressCommand command) {
        return getUser(userId).addAddress(command.streetNo(), command.streetName(), command.suburb(),
                command.city(), command.state(), command.postCode());
    }

    @Transactional
    public Address updateAddress(String userId, String addressId, AddressCommand command) {
        return getUser(userId)
                .updateAddress(addressId, command.streetNo(), command.streetName(), command.suburb(),
                        command.city(), command.state(), command.postCode())
                .orElseThrow(() -> addressNotFound(addressId));
    }

    @Transactional
    public void removeAddress(String userId, String addressId) {
        if (!getUser(userId).removeAddress(addressId)) {
            throw addressNotFound(addressId);
        }
    }

    private static ResourceNotFoundException addressNotFound(String addressId) {
        return new ResourceNotFoundException("Address not found: " + addressId);
    }
}

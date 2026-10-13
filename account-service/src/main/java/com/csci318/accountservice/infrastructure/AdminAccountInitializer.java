package com.csci318.accountservice.infrastructure;

import com.csci318.accountservice.domain.Role;
import com.csci318.accountservice.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the single ADMIN account at startup from configuration (security.admin.*, ADMIN_* environment
 * variables) if no user with that username exists yet. POST /users only ever creates customers.
 */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String email;
    private final String phone;

    public AdminAccountInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                   @Value("${security.admin.username}") String username,
                                   @Value("${security.admin.password}") String password,
                                   @Value("${security.admin.email}") String email,
                                   @Value("${security.admin.phone}") String phone) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("security.admin.password (ADMIN_PASSWORD) must be set");
        }
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phone = phone;
    }

    @Override
    public void run(ApplicationArguments args) {
        userRepository.findByUsername(username).ifPresentOrElse(
                existing -> {
                    if (existing.getRole() != Role.ADMIN) {
                        log.warn("Admin username '{}' belongs to a non-admin account; no admin account created",
                                username);
                    }
                },
                () -> {
                    userRepository.save(User.registerAdmin(username, email, phone, passwordEncoder.encode(password)));
                    log.info("Created admin account '{}'", username);
                });
    }
}

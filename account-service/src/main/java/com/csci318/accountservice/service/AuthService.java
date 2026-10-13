package com.csci318.accountservice.service;

import com.csci318.accountservice.domain.User;
import com.csci318.accountservice.domain.ValidationException;
import com.csci318.accountservice.infrastructure.JwtTokenService;
import com.csci318.accountservice.infrastructure.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A2: authenticates a user with username and password and issues a JWT.
 */
@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid username or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public AuthenticationResult login(String username, String password) {
        requireText(username, "username");
        requireText(password, "password");

        // Same error for an unknown username and a wrong password.
        User user = userRepository.findByUsername(username)
                .filter(u -> passwordEncoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_CREDENTIALS));

        JwtTokenService.IssuedToken token = tokenService.issue(user.getId(), user.getRole());
        return new AuthenticationResult(token.token(), token.expiresAt(), user.getId(), user.getRole());
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required");
        }
    }
}

package com.csci318.accountservice.presentation;

import com.csci318.accountservice.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** A2: authenticate a registered user and return a JWT. */
    @PostMapping("/login")
    public AuthenticationResponse login(@RequestBody LoginRequest request) {
        return AuthenticationResponse.from(authService.login(request.username(), request.password()));
    }
}

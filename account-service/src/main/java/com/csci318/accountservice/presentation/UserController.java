package com.csci318.accountservice.presentation;

import com.csci318.accountservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** A1: register a new customer account. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@RequestBody UserRegistrationRequest request) {
        return UserResponse.from(userService.registerUser(request.toCommand()));
    }

    /** A1: retrieve account and profile information. */
    @GetMapping("/{userId}")
    public UserResponse getUser(@PathVariable String userId) {
        return UserResponse.from(userService.getUser(userId));
    }

    /** A1: update customer profile information. */
    @PutMapping("/{userId}/profile")
    public UserResponse updateProfile(@PathVariable String userId, @RequestBody ProfileUpdateRequest request) {
        return UserResponse.from(userService.updateProfile(userId, request.toCommand()));
    }
}

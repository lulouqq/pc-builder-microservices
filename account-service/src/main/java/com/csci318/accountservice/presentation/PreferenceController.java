package com.csci318.accountservice.presentation;

import com.csci318.accountservice.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/{userId}/preferences")
public class PreferenceController {

    private final UserService userService;

    public PreferenceController(UserService userService) {
        this.userService = userService;
    }

    /** A3: retrieve saved recommendation preferences. */
    @GetMapping
    public PreferenceResponse getPreference(@PathVariable String userId) {
        return PreferenceResponse.from(userService.getUser(userId));
    }

    /** A3: save or update recommendation preferences. */
    @PutMapping
    public PreferenceResponse updatePreference(@PathVariable String userId,
                                               @RequestBody PreferenceUpdateRequest request) {
        return PreferenceResponse.from(userService.updatePreference(userId, request.toCommand()));
    }
}

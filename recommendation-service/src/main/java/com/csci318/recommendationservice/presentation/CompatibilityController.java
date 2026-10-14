package com.csci318.recommendationservice.presentation;

import com.csci318.recommendationservice.service.CompatibilityService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CompatibilityController {

    private final CompatibilityService compatibilityService;

    public CompatibilityController(CompatibilityService compatibilityService) {
        this.compatibilityService = compatibilityService;
    }

    /** R2 (public): always 200 when the check can be made, with compatible = false and conflicts if any. */
    @PostMapping("/compatibility/validate")
    public CompatibilityResponse validate(@RequestBody CompatibilityRequest request) {
        return CompatibilityResponse.from(compatibilityService.validate(request.productIds()));
    }
}

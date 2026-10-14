package com.csci318.recommendationservice.service;

import java.util.Optional;

/**
 * Port to Account Service (A3 preferences).
 */
public interface AccountClient {

    /**
     * The customer's saved recommendation preference, or empty when none is saved.
     * The customer's Authorization header is forwarded, because the endpoint is owner-only.
     *
     * @throws AuthenticationRequiredException when Account Service rejects the token (401)
     * @throws org.springframework.security.access.AccessDeniedException when Account Service denies access (403)
     * @throws ResourceNotFoundException   when Account Service does not know the user (404)
     * @throws ServiceUnavailableException when Account Service times out, cannot be reached or fails
     */
    Optional<String> getSavedPreference(String userId, String authorizationHeader);
}

package com.csci318.orderservice.service;

import java.util.List;

/**
 * Inter-service query to Account Service for a customer's saved delivery addresses (O2).
 */
public interface AccountClient {

    /**
     * The ids of the user's saved addresses. The customer's Authorization header is forwarded.
     *
     * @throws ServiceUnavailableException when Account Service cannot be reached
     */
    List<String> getSavedAddressIds(String userId, String authorizationHeader);
}

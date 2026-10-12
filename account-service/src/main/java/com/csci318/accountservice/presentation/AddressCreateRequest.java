package com.csci318.accountservice.presentation;

import com.csci318.accountservice.service.AddressCommand;

/**
 * Request body for POST /users/{userId}/addresses. All fields are required.
 */
public record AddressCreateRequest(
        Integer streetNo,
        String streetName,
        String suburb,
        String city,
        String state,
        Integer postCode) {

    public AddressCommand toCommand() {
        return new AddressCommand(streetNo, streetName, suburb, city, state, postCode);
    }
}

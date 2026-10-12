package com.csci318.accountservice.presentation;

import com.csci318.accountservice.service.AddressCommand;

/**
 * Request body for PUT /users/{userId}/addresses/{addressId}. Replaces the whole address; all fields are required.
 */
public record AddressUpdateRequest(
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

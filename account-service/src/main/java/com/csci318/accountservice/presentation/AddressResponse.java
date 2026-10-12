package com.csci318.accountservice.presentation;

import com.csci318.accountservice.domain.Address;

/**
 * A saved delivery address. The id is what Order Service stores as deliveryAddressId (O2).
 */
public record AddressResponse(
        String id,
        Integer streetNo,
        String streetName,
        String suburb,
        String city,
        String state,
        Integer postCode) {

    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getStreetNo(),
                address.getStreetName(),
                address.getSuburb(),
                address.getCity(),
                address.getState(),
                address.getPostCode());
    }
}

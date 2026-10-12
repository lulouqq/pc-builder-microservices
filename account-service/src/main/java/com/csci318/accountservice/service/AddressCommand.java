package com.csci318.accountservice.service;

/**
 * Input for adding or replacing a saved delivery address (A1). All fields are required.
 */
public record AddressCommand(
        Integer streetNo,
        String streetName,
        String suburb,
        String city,
        String state,
        Integer postCode) {
}

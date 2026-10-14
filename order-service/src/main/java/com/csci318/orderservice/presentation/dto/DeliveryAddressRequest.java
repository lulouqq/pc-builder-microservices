package com.csci318.orderservice.presentation.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * addressId is the id of one of the customer's saved addresses in Account Service.
 */
public record DeliveryAddressRequest(@NotBlank String addressId) {
}

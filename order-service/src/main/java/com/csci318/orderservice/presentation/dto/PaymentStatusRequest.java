package com.csci318.orderservice.presentation.dto;

import com.csci318.orderservice.domain.PaymentStatus;
import jakarta.validation.constraints.NotNull;

/**
 * paymentStatus is the payment result: PAID or FAILED.
 */
public record PaymentStatusRequest(@NotNull PaymentStatus paymentStatus) {
}

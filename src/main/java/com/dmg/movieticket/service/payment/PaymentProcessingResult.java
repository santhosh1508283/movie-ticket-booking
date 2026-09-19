package com.dmg.movieticket.service.payment;

public record PaymentProcessingResult(
        boolean successful,
        String providerReference,
        String failureReason
) {
}
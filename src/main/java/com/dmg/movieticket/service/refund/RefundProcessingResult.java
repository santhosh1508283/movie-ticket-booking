package com.dmg.movieticket.service.refund;

public record RefundProcessingResult(
        boolean successful,
        String providerReference,
        String failureReason
) {
}
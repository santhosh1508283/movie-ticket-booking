package com.dmg.movieticket.dto.response;

import com.dmg.movieticket.entity.RefundStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundResponse(
        Long refundId,
        Long bookingId,
        BigDecimal amount,
        BigDecimal refundPercentage,
        RefundStatus status,
        String providerReference,
        LocalDateTime createdAt,
        LocalDateTime processedAt
) {
}
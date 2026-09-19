package com.dmg.movieticket.dto.response;

import com.dmg.movieticket.entity.BookingStatus;
import com.dmg.movieticket.entity.PaymentMethod;
import com.dmg.movieticket.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long paymentId,
        Long bookingId,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        String providerReference,
        BookingStatus bookingStatus,
        LocalDateTime createdAt
) {
}
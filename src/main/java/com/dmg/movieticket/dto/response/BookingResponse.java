package com.dmg.movieticket.dto.response;

import com.dmg.movieticket.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
        Long id,
        String bookingReference,
        Long showId,
        String movieTitle,
        String theaterName,
        String screenName,
        LocalDateTime showStartTime,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        String appliedDiscountCode,
        BigDecimal totalAmount,
        BookingStatus status,
        List<BookingItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime confirmedAt,
        LocalDateTime cancelledAt
) {
}
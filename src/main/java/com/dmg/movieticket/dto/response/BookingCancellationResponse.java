package com.dmg.movieticket.dto.response;

import com.dmg.movieticket.entity.BookingStatus;

public record BookingCancellationResponse(
        Long bookingId,
        String bookingReference,
        BookingStatus bookingStatus,
        RefundResponse refund
) {
}
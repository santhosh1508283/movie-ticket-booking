package com.dmg.movieticket.dto.response;

import com.dmg.movieticket.entity.SeatType;

import java.math.BigDecimal;

public record BookingItemResponse(
        Long showSeatId,
        String seatLabel,
        SeatType seatType,
        BigDecimal price
) {
}
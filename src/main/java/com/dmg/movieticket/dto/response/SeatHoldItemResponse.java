package com.dmg.movieticket.dto.response;

import com.dmg.movieticket.entity.SeatType;

import java.math.BigDecimal;

public record SeatHoldItemResponse(
        Long showSeatId,
        String seatLabel,
        SeatType seatType,
        BigDecimal price
) {
}
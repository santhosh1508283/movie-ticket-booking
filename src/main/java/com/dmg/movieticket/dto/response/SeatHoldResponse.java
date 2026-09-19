package com.dmg.movieticket.dto.response;

import com.dmg.movieticket.entity.HoldStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SeatHoldResponse(
        Long holdId,
        Long showId,
        HoldStatus status,
        LocalDateTime expiresAt,
        BigDecimal totalAmount,
        List<SeatHoldItemResponse> seats
) {
}
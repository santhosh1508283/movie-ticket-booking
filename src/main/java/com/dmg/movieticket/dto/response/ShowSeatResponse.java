package com.dmg.movieticket.dto.response;

import com.dmg.movieticket.entity.SeatType;
import com.dmg.movieticket.entity.ShowSeatStatus;

import java.math.BigDecimal;

public record ShowSeatResponse(
        Long showSeatId,
        Long seatId,
        String seatLabel,
        SeatType seatType,
        BigDecimal price,
        ShowSeatStatus status
) {
}
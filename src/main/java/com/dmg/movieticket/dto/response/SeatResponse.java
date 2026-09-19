package com.dmg.movieticket.dto.response;

import com.dmg.movieticket.entity.SeatType;

public record SeatResponse(
        Long id,
        String rowLabel,
        Integer seatNumber,
        SeatType seatType,
        Long screenId,
        String screenName
) {
}
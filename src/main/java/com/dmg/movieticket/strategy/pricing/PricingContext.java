package com.dmg.movieticket.strategy.pricing;

import com.dmg.movieticket.entity.SeatType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PricingContext(
        SeatType seatType,
        BigDecimal basePrice,
        LocalDateTime showTime
) {
}
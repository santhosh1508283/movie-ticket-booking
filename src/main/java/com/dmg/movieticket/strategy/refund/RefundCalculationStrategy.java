package com.dmg.movieticket.strategy.refund;

import com.dmg.movieticket.entity.Booking;

import java.time.LocalDateTime;

public interface RefundCalculationStrategy {

    RefundCalculationResult calculate(
            Booking booking,
            LocalDateTime cancellationTime
    );
}
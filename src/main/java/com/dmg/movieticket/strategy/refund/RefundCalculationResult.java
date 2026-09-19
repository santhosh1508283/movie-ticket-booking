package com.dmg.movieticket.strategy.refund;

import java.math.BigDecimal;

public record RefundCalculationResult(
        BigDecimal refundPercentage,
        BigDecimal refundAmount
) {
}
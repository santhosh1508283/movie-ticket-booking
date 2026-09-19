package com.dmg.movieticket.dto.response;

import java.math.BigDecimal;

public record RefundPolicyResponse(
        Long id,
        Integer hoursBeforeShow,
        BigDecimal refundPercentage,
        Boolean active
) {
}
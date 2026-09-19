package com.dmg.movieticket.dto.response;

import com.dmg.movieticket.entity.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DiscountCodeResponse(
        Long id,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minimumOrderAmount,
        BigDecimal maximumDiscountAmount,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        Boolean active
) {
}
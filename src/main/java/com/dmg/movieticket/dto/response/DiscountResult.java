package com.dmg.movieticket.dto.response;

import java.math.BigDecimal;

public record DiscountResult(
        String code,
        BigDecimal discountAmount
) {
}
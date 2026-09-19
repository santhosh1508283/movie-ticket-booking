package com.dmg.movieticket.dto.request;

import com.dmg.movieticket.entity.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateDiscountCodeRequest(

        @NotBlank
        @Size(max = 50)
        String code,

        @NotNull
        DiscountType discountType,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal discountValue,

        @DecimalMin("0.00")
        BigDecimal minimumOrderAmount,

        @DecimalMin("0.00")
        BigDecimal maximumDiscountAmount,

        @NotNull
        LocalDateTime validFrom,

        @NotNull
        LocalDateTime validUntil

) {
}
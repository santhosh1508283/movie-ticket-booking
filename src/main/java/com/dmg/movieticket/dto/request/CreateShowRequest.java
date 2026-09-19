package com.dmg.movieticket.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateShowRequest(

        @NotNull(message = "Movie id is required")
        Long movieId,

        @NotNull(message = "Screen id is required")
        Long screenId,

        @NotNull(message = "Show start time is required")
        LocalDateTime startTime,

        @NotNull(message = "Regular seat base price is required")
        @DecimalMin(value = "0.01", message = "Regular seat price must be greater than 0")
        BigDecimal regularBasePrice,

        @NotNull(message = "Premium seat base price is required")
        @DecimalMin(value = "0.01", message = "Premium seat price must be greater than 0")
        BigDecimal premiumBasePrice

) {
}
package com.dmg.movieticket.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBookingRequest(

        @NotNull(message = "Seat hold id is required")
        Long holdId,

        @Size(max = 50, message = "Discount code must not exceed 50 characters")
        String discountCode

) {
}
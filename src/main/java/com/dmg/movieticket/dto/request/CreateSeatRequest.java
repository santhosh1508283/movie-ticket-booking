package com.dmg.movieticket.dto.request;

import com.dmg.movieticket.entity.SeatType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSeatRequest(

        @NotBlank(message = "Row label is required")
        @Size(max = 10, message = "Row label must not exceed 10 characters")
        String rowLabel,

        @NotNull(message = "Seat number is required")
        @Min(value = 1, message = "Seat number must be greater than 0")
        Integer seatNumber,

        @NotNull(message = "Seat type is required")
        SeatType seatType

) {
}
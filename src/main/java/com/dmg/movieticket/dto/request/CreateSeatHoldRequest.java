package com.dmg.movieticket.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateSeatHoldRequest(

        @NotNull(message = "Show id is required")
        Long showId,

        @NotEmpty(message = "At least one show seat is required")
        List<@NotNull Long> showSeatIds

) {
}
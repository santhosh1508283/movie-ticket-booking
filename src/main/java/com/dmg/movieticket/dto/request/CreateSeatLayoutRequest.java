package com.dmg.movieticket.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateSeatLayoutRequest(

        @NotNull(message = "Screen id is required")
        Long screenId,

        @NotEmpty(message = "At least one seat is required")
        List<@Valid CreateSeatRequest> seats

) {
}
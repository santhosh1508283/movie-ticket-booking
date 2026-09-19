package com.dmg.movieticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateScreenRequest(

        @NotBlank(message = "Screen name is required")
        @Size(max = 100, message = "Screen name must not exceed 100 characters")
        String name,

        @NotNull(message = "Theater id is required")
        @Positive(message = "Theater id must be greater than 0")
        Long theaterId

) {
}

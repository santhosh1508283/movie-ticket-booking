package com.dmg.movieticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateTheaterRequest(

        @NotBlank(message = "Theater name is required")
        @Size(max = 150, message = "Theater name must not exceed 150 characters")
        String name,

        @NotBlank(message = "Address is required")
        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        @Size(max = 100, message = "Landmark must not exceed 100 characters")
        String landmark,

        @Size(max = 20, message = "Postal code must not exceed 20 characters")
        String postalCode,

        @NotNull(message = "City id is required")
        @Positive(message = "City id must be greater than 0")
        Long cityId

) {
}

package com.dmg.movieticket.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMovieRequest(

        @NotBlank(message = "Movie title is required")
        @Size(max = 150, message = "Movie title must not exceed 150 characters")
        String title,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @NotNull(message = "Duration is required")
        @Min(value = 1, message = "Duration must be greater than 0")
        Integer durationMinutes,

        @NotBlank(message = "Language is required")
        @Size(max = 50, message = "Language must not exceed 50 characters")
        String language,

        @NotBlank(message = "Genre is required")
        @Size(max = 50, message = "Genre must not exceed 50 characters")
        String genre

) {
}
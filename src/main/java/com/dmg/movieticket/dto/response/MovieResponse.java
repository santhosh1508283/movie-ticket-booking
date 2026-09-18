package com.dmg.movieticket.dto.response;

public record MovieResponse(
        Long id,
        String title,
        String description,
        Integer durationMinutes,
        String language,
        String genre
) {
}
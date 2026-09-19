package com.dmg.movieticket.dto.response;

import com.dmg.movieticket.entity.ShowStatus;

import java.time.LocalDateTime;

public record ShowResponse(
        Long id,
        Long movieId,
        String movieTitle,
        Long screenId,
        String screenName,
        Long theaterId,
        String theaterName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        ShowStatus status
) {
}
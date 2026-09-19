package com.dmg.movieticket.service;

import com.dmg.movieticket.dto.request.CreateShowRequest;
import com.dmg.movieticket.dto.response.ShowResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface ShowService {

    ShowResponse createShow(CreateShowRequest request);

    ShowResponse getShowById(Long showId);

    List<ShowResponse> getShowsByMovie(Long movieId);

    List<ShowResponse> getShowsByScreen(Long screenId);

    List<ShowResponse> getShowsByMovieAndDateRange(
            Long movieId,
            LocalDateTime start,
            LocalDateTime end
    );
}
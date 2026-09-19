package com.dmg.movieticket.controller;

import com.dmg.movieticket.dto.request.CreateShowRequest;
import com.dmg.movieticket.dto.response.ShowResponse;
import com.dmg.movieticket.service.ShowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @PostMapping
    public ResponseEntity<ShowResponse> createShow(
            @Valid @RequestBody CreateShowRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(showService.createShow(request));
    }

    @GetMapping("/{showId}")
    public ResponseEntity<ShowResponse> getShow(
            @PathVariable Long showId
    ) {
        return ResponseEntity.ok(
                showService.getShowById(showId)
        );
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ShowResponse>> getShowsByMovie(
            @PathVariable Long movieId
    ) {
        return ResponseEntity.ok(
                showService.getShowsByMovie(movieId)
        );
    }

    @GetMapping("/screen/{screenId}")
    public ResponseEntity<List<ShowResponse>> getShowsByScreen(
            @PathVariable Long screenId
    ) {
        return ResponseEntity.ok(
                showService.getShowsByScreen(screenId)
        );
    }

    @GetMapping("/movie/{movieId}/range")
    public ResponseEntity<List<ShowResponse>> getShowsByMovieAndDateRange(
            @PathVariable Long movieId,
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end
    ) {
        return ResponseEntity.ok(
                showService.getShowsByMovieAndDateRange(
                        movieId,
                        start,
                        end
                )
        );
    }
}
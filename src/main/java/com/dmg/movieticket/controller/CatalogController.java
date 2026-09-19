package com.dmg.movieticket.controller;

import com.dmg.movieticket.dto.response.MovieResponse;
import com.dmg.movieticket.dto.response.ShowResponse;
import com.dmg.movieticket.dto.response.ShowSeatResponse;
import com.dmg.movieticket.dto.response.TheaterResponse;
import com.dmg.movieticket.service.MovieService;
import com.dmg.movieticket.service.ShowSeatService;
import com.dmg.movieticket.service.ShowService;
import com.dmg.movieticket.service.TheaterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final MovieService movieService;
    private final TheaterService theaterService;
    private final ShowService showService;
    private final ShowSeatService showSeatService;

    @GetMapping("/movies")
    public ResponseEntity<List<MovieResponse>> getMovies() {
        return ResponseEntity.ok(
                movieService.getAllMovies()
        );
    }

    @GetMapping("/movies/search")
    public ResponseEntity<List<MovieResponse>> searchMovies(
            @RequestParam String title
    ) {
        return ResponseEntity.ok(
                movieService.searchMovies(title)
        );
    }

    @GetMapping("/movies/{movieId}")
    public ResponseEntity<MovieResponse> getMovie(
            @PathVariable Long movieId
    ) {
        return ResponseEntity.ok(
                movieService.getMovieById(movieId)
        );
    }

    @GetMapping("/cities/{cityId}/theaters")
    public ResponseEntity<List<TheaterResponse>> getTheatersByCity(
            @PathVariable Long cityId
    ) {
        return ResponseEntity.ok(
                theaterService.getTheatersByCity(cityId)
        );
    }

    @GetMapping("/movies/{movieId}/shows")
    public ResponseEntity<List<ShowResponse>> getShowsByMovie(
            @PathVariable Long movieId
    ) {
        return ResponseEntity.ok(
                showService.getShowsByMovie(movieId)
        );
    }

    @GetMapping("/movies/{movieId}/shows/range")
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

    @GetMapping("/shows/{showId}")
    public ResponseEntity<ShowResponse> getShow(
            @PathVariable Long showId
    ) {
        return ResponseEntity.ok(
                showService.getShowById(showId)
        );
    }

    @GetMapping("/shows/{showId}/seats")
    public ResponseEntity<List<ShowSeatResponse>> getShowSeats(
            @PathVariable Long showId
    ) {
        return ResponseEntity.ok(
                showSeatService.getSeatsByShow(showId)
        );
    }
}
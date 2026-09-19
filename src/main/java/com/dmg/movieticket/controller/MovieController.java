package com.dmg.movieticket.controller;

import com.dmg.movieticket.dto.request.CreateMovieRequest;
import com.dmg.movieticket.dto.response.MovieResponse;
import com.dmg.movieticket.service.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @PostMapping
    public ResponseEntity<MovieResponse> createMovie(
            @Valid @RequestBody CreateMovieRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(movieService.createMovie(request));
    }

    @GetMapping("/{movieId}")
    public ResponseEntity<MovieResponse> getMovie(
            @PathVariable Long movieId
    ) {
        return ResponseEntity.ok(
                movieService.getMovieById(movieId)
        );
    }

    @GetMapping
    public ResponseEntity<List<MovieResponse>> getAllMovies() {
        return ResponseEntity.ok(
                movieService.getAllMovies()
        );
    }

    @GetMapping("/search")
    public ResponseEntity<List<MovieResponse>> searchMovies(
            @RequestParam String title
    ) {
        return ResponseEntity.ok(
                movieService.searchMovies(title)
        );
    }

    @GetMapping("/language")
    public ResponseEntity<List<MovieResponse>> getByLanguage(
            @RequestParam String language
    ) {
        return ResponseEntity.ok(
                movieService.getMoviesByLanguage(language)
        );
    }

    @GetMapping("/genre")
    public ResponseEntity<List<MovieResponse>> getByGenre(
            @RequestParam String genre
    ) {
        return ResponseEntity.ok(
                movieService.getMoviesByGenre(genre)
        );
    }
}
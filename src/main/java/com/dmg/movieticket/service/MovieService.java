package com.dmg.movieticket.service;

import com.dmg.movieticket.dto.request.CreateMovieRequest;
import com.dmg.movieticket.dto.response.MovieResponse;

import java.util.List;

public interface MovieService {

    MovieResponse createMovie(CreateMovieRequest request);

    MovieResponse getMovieById(Long movieId);

    List<MovieResponse> getAllMovies();

    List<MovieResponse> searchMovies(String title);

    List<MovieResponse> getMoviesByLanguage(String language);

    List<MovieResponse> getMoviesByGenre(String genre);
}
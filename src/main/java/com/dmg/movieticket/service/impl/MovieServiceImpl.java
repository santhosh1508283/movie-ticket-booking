package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.CreateMovieRequest;
import com.dmg.movieticket.dto.response.MovieResponse;
import com.dmg.movieticket.entity.Movie;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.mapper.MovieMapper;
import com.dmg.movieticket.repository.MovieRepository;
import com.dmg.movieticket.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;

    @Override
    @Transactional
    public MovieResponse createMovie(CreateMovieRequest request) {

        Movie movie = Movie.builder()
                .title(request.title().trim())
                .description(
                        request.description() != null
                                ? request.description().trim()
                                : null
                )
                .durationMinutes(request.durationMinutes())
                .language(request.language().trim())
                .genre(request.genre().trim())
                .build();

        return movieMapper.toResponse(movieRepository.save(movie));
    }

    @Override
    @Transactional(readOnly = true)
    public MovieResponse getMovieById(Long movieId) {

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Movie not found with id: " + movieId
                ));

        return movieMapper.toResponse(movie);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieResponse> getAllMovies() {

        return movieRepository.findAll()
                .stream()
                .map(movieMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieResponse> searchMovies(String title) {

        return movieRepository.findByTitleContainingIgnoreCase(title.trim())
                .stream()
                .map(movieMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieResponse> getMoviesByLanguage(String language) {

        return movieRepository.findByLanguageIgnoreCase(language.trim())
                .stream()
                .map(movieMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieResponse> getMoviesByGenre(String genre) {

        return movieRepository.findByGenreIgnoreCase(genre.trim())
                .stream()
                .map(movieMapper::toResponse)
                .toList();
    }
}
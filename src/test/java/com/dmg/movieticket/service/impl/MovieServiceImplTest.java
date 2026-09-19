package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.CreateMovieRequest;
import com.dmg.movieticket.dto.response.MovieResponse;
import com.dmg.movieticket.entity.Movie;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.mapper.MovieMapper;
import com.dmg.movieticket.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MovieServiceImplTest {

    private MovieRepository movieRepository;
    private MovieServiceImpl service;

    @BeforeEach
    void setUp() {
        movieRepository = mock(MovieRepository.class);
        service = new MovieServiceImpl(movieRepository, Mappers.getMapper(MovieMapper.class));
    }

    @Test
    void createsMovieWithNormalizedFieldsAndGeneratedId() {
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> {
            Movie movie = invocation.getArgument(0);
            assertNull(movie.getId());
            movie.setId(1L);
            return movie;
        });

        MovieResponse response = service.createMovie(new CreateMovieRequest(
                " Interstellar ", " Space exploration ", 169, " English ", " Science Fiction "));

        assertEquals(expectedResponse(1L, "Interstellar"), response);
        verify(movieRepository).save(any(Movie.class));
    }

    @Test
    void allowsMissingDescription() {
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MovieResponse response = service.createMovie(new CreateMovieRequest(
                "Interstellar", null, 169, "English", "Science Fiction"));

        assertEquals(new MovieResponse(null, "Interstellar", null, 169, "English", "Science Fiction"),
                response);
    }

    @Test
    void trimsWhitespaceOnlyDescriptionToEmptyString() {
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MovieResponse response = service.createMovie(new CreateMovieRequest(
                "Interstellar", "   ", 169, "English", "Science Fiction"));

        assertEquals("", response.description());
    }

    @Test
    void findsMovieById() {
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie(1L, "Interstellar")));

        assertEquals(expectedResponse(1L, "Interstellar"), service.getMovieById(1L));
    }

    @Test
    void rejectsMissingMovie() {
        when(movieRepository.findById(99L)).thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> service.getMovieById(99L));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        assertEquals("Movie not found with id: 99", exception.getMessage());
    }

    @Test
    void listsAllMoviesInRepositoryOrder() {
        when(movieRepository.findAll()).thenReturn(List.of(movie(1L, "Interstellar"), movie(2L, "Gravity")));

        assertEquals(List.of(expectedResponse(1L, "Interstellar"), expectedResponse(2L, "Gravity")),
                service.getAllMovies());
    }

    @Test
    void returnsEmptyListWhenNoMoviesExist() {
        when(movieRepository.findAll()).thenReturn(List.of());

        assertTrue(service.getAllMovies().isEmpty());
        verify(movieRepository).findAll();
    }

    @Test
    void trimsTitleSearchAndMapsResults() {
        when(movieRepository.findByTitleContainingIgnoreCase("inter")).thenReturn(List.of(movie(1L, "Interstellar")));

        assertEquals(List.of(expectedResponse(1L, "Interstellar")), service.searchMovies(" inter "));
        verify(movieRepository).findByTitleContainingIgnoreCase("inter");
    }

    @Test
    void returnsEmptyListWhenTitleDoesNotMatch() {
        when(movieRepository.findByTitleContainingIgnoreCase("unknown")).thenReturn(List.of());

        assertTrue(service.searchMovies(" unknown ").isEmpty());
        verify(movieRepository).findByTitleContainingIgnoreCase("unknown");
    }

    @Test
    void trimsLanguageFilterAndMapsResults() {
        when(movieRepository.findByLanguageIgnoreCase("english")).thenReturn(List.of(movie(1L, "Interstellar")));

        assertEquals(List.of(expectedResponse(1L, "Interstellar")), service.getMoviesByLanguage(" english "));
        verify(movieRepository).findByLanguageIgnoreCase("english");
    }

    @Test
    void returnsEmptyListWhenLanguageDoesNotMatch() {
        when(movieRepository.findByLanguageIgnoreCase("French")).thenReturn(List.of());

        assertTrue(service.getMoviesByLanguage(" French ").isEmpty());
        verify(movieRepository).findByLanguageIgnoreCase("French");
    }

    @Test
    void trimsGenreFilterAndMapsResults() {
        when(movieRepository.findByGenreIgnoreCase("science fiction")).thenReturn(List.of(movie(1L, "Interstellar")));

        assertEquals(List.of(expectedResponse(1L, "Interstellar")), service.getMoviesByGenre(" science fiction "));
        verify(movieRepository).findByGenreIgnoreCase("science fiction");
    }

    @Test
    void returnsEmptyListWhenGenreDoesNotMatch() {
        when(movieRepository.findByGenreIgnoreCase("Comedy")).thenReturn(List.of());

        assertTrue(service.getMoviesByGenre(" Comedy ").isEmpty());
        verify(movieRepository).findByGenreIgnoreCase("Comedy");
    }

    private Movie movie(Long id, String title) {
        return Movie.builder().id(id).title(title).description("Space exploration")
                .durationMinutes(169).language("English").genre("Science Fiction").build();
    }

    private MovieResponse expectedResponse(Long id, String title) {
        return new MovieResponse(id, title, "Space exploration", 169, "English", "Science Fiction");
    }
}

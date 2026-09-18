package com.dmg.movieticket.repository;

import com.dmg.movieticket.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    List<Movie> findByTitleContainingIgnoreCase(String title);

    List<Movie> findByLanguageIgnoreCase(String language);

    List<Movie> findByGenreIgnoreCase(String genre);
}
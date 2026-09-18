package com.dmg.movieticket.mapper;

import com.dmg.movieticket.dto.response.MovieResponse;
import com.dmg.movieticket.entity.Movie;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MovieMapper {

    MovieResponse toResponse(Movie movie);
}
package com.dmg.movieticket.mapper;

import com.dmg.movieticket.dto.response.ShowResponse;
import com.dmg.movieticket.entity.Show;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShowMapper {

    @Mapping(source = "movie.id", target = "movieId")
    @Mapping(source = "movie.title", target = "movieTitle")
    @Mapping(source = "screen.id", target = "screenId")
    @Mapping(source = "screen.name", target = "screenName")
    @Mapping(source = "screen.theater.id", target = "theaterId")
    @Mapping(source = "screen.theater.name", target = "theaterName")
    ShowResponse toResponse(Show show);
}
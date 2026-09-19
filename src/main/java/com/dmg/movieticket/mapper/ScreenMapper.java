package com.dmg.movieticket.mapper;

import com.dmg.movieticket.dto.response.ScreenResponse;
import com.dmg.movieticket.entity.Screen;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ScreenMapper {

    @Mapping(target = "theaterId", source = "theater.id")
    @Mapping(target = "theaterName", source = "theater.name")
    ScreenResponse toResponse(Screen screen);
}

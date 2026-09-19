package com.dmg.movieticket.mapper;

import com.dmg.movieticket.dto.response.TheaterResponse;
import com.dmg.movieticket.entity.Theater;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TheaterMapper {

    @Mapping(target = "cityId", source = "city.id")
    @Mapping(target = "cityName", source = "city.name")
    TheaterResponse toResponse(Theater theater);
}

package com.dmg.movieticket.mapper;

import com.dmg.movieticket.dto.response.CityResponse;
import com.dmg.movieticket.entity.City;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CityMapper {

    CityResponse toResponse(City city);
}
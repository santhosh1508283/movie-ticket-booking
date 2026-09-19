package com.dmg.movieticket.mapper;

import com.dmg.movieticket.dto.response.SeatResponse;
import com.dmg.movieticket.entity.Seat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SeatMapper {

    @Mapping(source = "screen.id", target = "screenId")
    @Mapping(source = "screen.name", target = "screenName")
    SeatResponse toResponse(Seat seat);
}
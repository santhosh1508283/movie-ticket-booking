package com.dmg.movieticket.mapper;

import com.dmg.movieticket.dto.response.ShowSeatResponse;
import com.dmg.movieticket.entity.ShowSeat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShowSeatMapper {

    @Mapping(source = "id", target = "showSeatId")
    @Mapping(source = "seat.id", target = "seatId")
    @Mapping(
            expression = "java(showSeat.getSeat().getRowLabel() + showSeat.getSeat().getSeatNumber())",
            target = "seatLabel"
    )
    @Mapping(source = "seat.seatType", target = "seatType")
    ShowSeatResponse toResponse(ShowSeat showSeat);
}
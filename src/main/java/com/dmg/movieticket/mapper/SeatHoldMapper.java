package com.dmg.movieticket.mapper;

import com.dmg.movieticket.dto.response.SeatHoldItemResponse;
import com.dmg.movieticket.dto.response.SeatHoldResponse;
import com.dmg.movieticket.entity.SeatHold;
import com.dmg.movieticket.entity.SeatHoldItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SeatHoldMapper {

    @Mapping(source = "id", target = "holdId")
    @Mapping(source = "show.id", target = "showId")
    @Mapping(source = "items", target = "seats")
    @Mapping(
            target = "totalAmount",
            expression = "java(seatHold.getItems().stream()" +
                    ".map(com.dmg.movieticket.entity.SeatHoldItem::getPrice)" +
                    ".reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add))"
    )
    SeatHoldResponse toResponse(SeatHold seatHold);

    @Mapping(source = "showSeat.id", target = "showSeatId")
    @Mapping(
            target = "seatLabel",
            expression = "java(item.getShowSeat().getSeat().getRowLabel()" +
                    " + item.getShowSeat().getSeat().getSeatNumber())"
    )
    @Mapping(source = "showSeat.seat.seatType", target = "seatType")
    SeatHoldItemResponse toItemResponse(SeatHoldItem item);
}
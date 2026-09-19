package com.dmg.movieticket.mapper;

import com.dmg.movieticket.dto.response.BookingItemResponse;
import com.dmg.movieticket.dto.response.BookingResponse;
import com.dmg.movieticket.entity.Booking;
import com.dmg.movieticket.entity.BookingItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(source = "show.id", target = "showId")
    @Mapping(source = "show.movie.title", target = "movieTitle")
    @Mapping(source = "show.screen.theater.name", target = "theaterName")
    @Mapping(source = "show.screen.name", target = "screenName")
    @Mapping(source = "show.startTime", target = "showStartTime")
    BookingResponse toResponse(Booking booking);

    @Mapping(source = "showSeat.id", target = "showSeatId")
    BookingItemResponse toItemResponse(BookingItem item);
}
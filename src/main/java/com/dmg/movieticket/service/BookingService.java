package com.dmg.movieticket.service;

import com.dmg.movieticket.dto.request.CreateBookingRequest;
import com.dmg.movieticket.dto.response.BookingResponse;

import java.util.List;

public interface BookingService {

    BookingResponse createBooking(
            CreateBookingRequest request
    );

    BookingResponse getBooking(Long bookingId);

    List<BookingResponse> getMyBookings();
}
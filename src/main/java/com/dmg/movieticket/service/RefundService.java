package com.dmg.movieticket.service;

import com.dmg.movieticket.dto.response.BookingCancellationResponse;

public interface RefundService {

    BookingCancellationResponse cancelBooking(
            Long bookingId
    );
}
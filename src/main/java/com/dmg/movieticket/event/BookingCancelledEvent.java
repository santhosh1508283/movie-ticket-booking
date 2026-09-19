package com.dmg.movieticket.event;

public record BookingCancelledEvent(
        Long bookingId
) {
}
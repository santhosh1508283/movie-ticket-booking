package com.dmg.movieticket.controller;

import com.dmg.movieticket.dto.request.CreateBookingRequest;
import com.dmg.movieticket.dto.response.BookingCancellationResponse;
import com.dmg.movieticket.dto.response.BookingResponse;
import com.dmg.movieticket.service.BookingService;
import com.dmg.movieticket.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final RefundService refundService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(bookingService.createBooking(request));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getBooking(
            @PathVariable Long bookingId
    ) {
        return ResponseEntity.ok(
                bookingService.getBooking(bookingId)
        );
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getMyBookings() {
        return ResponseEntity.ok(
                bookingService.getMyBookings()
        );
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingCancellationResponse> cancelBooking(
            @PathVariable Long bookingId
    ) {
        return ResponseEntity.ok(
                refundService.cancelBooking(bookingId)
        );
    }
}
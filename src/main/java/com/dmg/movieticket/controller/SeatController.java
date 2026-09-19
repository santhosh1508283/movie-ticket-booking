package com.dmg.movieticket.controller;

import com.dmg.movieticket.dto.request.CreateSeatLayoutRequest;
import com.dmg.movieticket.dto.response.SeatResponse;
import com.dmg.movieticket.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @PostMapping("/layout")
    public ResponseEntity<List<SeatResponse>> createSeatLayout(
            @Valid @RequestBody CreateSeatLayoutRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(seatService.createSeatLayout(request));
    }

    @GetMapping("/{seatId}")
    public ResponseEntity<SeatResponse> getSeat(
            @PathVariable Long seatId
    ) {
        return ResponseEntity.ok(
                seatService.getSeatById(seatId)
        );
    }

    @GetMapping("/screen/{screenId}")
    public ResponseEntity<List<SeatResponse>> getSeatsByScreen(
            @PathVariable Long screenId
    ) {
        return ResponseEntity.ok(
                seatService.getSeatsByScreen(screenId)
        );
    }
}
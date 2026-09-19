package com.dmg.movieticket.controller;

import com.dmg.movieticket.dto.request.CreateSeatHoldRequest;
import com.dmg.movieticket.dto.response.SeatHoldResponse;
import com.dmg.movieticket.service.SeatHoldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seat-holds")
@RequiredArgsConstructor
public class SeatHoldController {

    private final SeatHoldService seatHoldService;

    @PostMapping
    public ResponseEntity<SeatHoldResponse> createHold(
            @Valid @RequestBody CreateSeatHoldRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(seatHoldService.createHold(request));
    }

    @GetMapping("/{holdId}")
    public ResponseEntity<SeatHoldResponse> getHold(
            @PathVariable Long holdId
    ) {
        return ResponseEntity.ok(
                seatHoldService.getHold(holdId)
        );
    }

    @DeleteMapping("/{holdId}")
    public ResponseEntity<Void> releaseHold(
            @PathVariable Long holdId
    ) {
        seatHoldService.releaseHold(holdId);

        return ResponseEntity.noContent().build();
    }
}
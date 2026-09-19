package com.dmg.movieticket.controller;

import com.dmg.movieticket.dto.request.CreateTheaterRequest;
import com.dmg.movieticket.dto.response.TheaterResponse;
import com.dmg.movieticket.service.TheaterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/theaters")
@RequiredArgsConstructor
public class TheaterController {

    private final TheaterService theaterService;

    @PostMapping
    public ResponseEntity<TheaterResponse> createTheater(
            @Valid @RequestBody CreateTheaterRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(theaterService.createTheater(request));
    }

    @GetMapping("/{theaterId}")
    public ResponseEntity<TheaterResponse> getTheater(
            @PathVariable Long theaterId
    ) {
        return ResponseEntity.ok(
                theaterService.getTheaterById(theaterId)
        );
    }

    @GetMapping
    public ResponseEntity<List<TheaterResponse>> getAllTheaters() {
        return ResponseEntity.ok(
                theaterService.getAllTheaters()
        );
    }

    @GetMapping("/city/{cityId}")
    public ResponseEntity<List<TheaterResponse>> getByCity(
            @PathVariable Long cityId
    ) {
        return ResponseEntity.ok(
                theaterService.getTheatersByCity(cityId)
        );
    }

    @GetMapping("/search")
    public ResponseEntity<List<TheaterResponse>> searchTheaters(
            @RequestParam String name
    ) {
        return ResponseEntity.ok(
                theaterService.searchTheaters(name)
        );
    }
}
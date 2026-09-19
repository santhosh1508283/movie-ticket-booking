package com.dmg.movieticket.controller;

import com.dmg.movieticket.dto.request.CreateScreenRequest;
import com.dmg.movieticket.dto.response.ScreenResponse;
import com.dmg.movieticket.service.ScreenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/screens")
@RequiredArgsConstructor
public class ScreenController {

    private final ScreenService screenService;

    @PostMapping
    public ResponseEntity<ScreenResponse> createScreen(
            @Valid @RequestBody CreateScreenRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(screenService.createScreen(request));
    }

    @GetMapping("/{screenId}")
    public ResponseEntity<ScreenResponse> getScreen(
            @PathVariable Long screenId
    ) {
        return ResponseEntity.ok(
                screenService.getScreenById(screenId)
        );
    }

    @GetMapping
    public ResponseEntity<List<ScreenResponse>> getAllScreens() {
        return ResponseEntity.ok(
                screenService.getAllScreens()
        );
    }

    @GetMapping("/theater/{theaterId}")
    public ResponseEntity<List<ScreenResponse>> getByTheater(
            @PathVariable Long theaterId
    ) {
        return ResponseEntity.ok(
                screenService.getScreensByTheater(theaterId)
        );
    }
}
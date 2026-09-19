package com.dmg.movieticket.controller;

import com.dmg.movieticket.dto.request.CreateCityRequest;
import com.dmg.movieticket.dto.response.CityResponse;
import com.dmg.movieticket.service.CityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/cities")
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    @PostMapping
    public ResponseEntity<CityResponse> createCity(
            @Valid @RequestBody CreateCityRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cityService.createCity(request));
    }

    @GetMapping("/{cityId}")
    public ResponseEntity<CityResponse> getCity(
            @PathVariable Long cityId
    ) {
        return ResponseEntity.ok(
                cityService.getCityById(cityId)
        );
    }

    @GetMapping
    public ResponseEntity<List<CityResponse>> getAllCities() {
        return ResponseEntity.ok(
                cityService.getAllCities()
        );
    }
}
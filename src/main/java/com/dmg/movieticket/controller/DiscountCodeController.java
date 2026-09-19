package com.dmg.movieticket.controller;

import com.dmg.movieticket.dto.request.CreateDiscountCodeRequest;
import com.dmg.movieticket.dto.response.DiscountCodeResponse;
import com.dmg.movieticket.service.DiscountCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/discount-codes")
@RequiredArgsConstructor
public class DiscountCodeController {

    private final DiscountCodeService discountCodeService;

    @PostMapping
    public ResponseEntity<DiscountCodeResponse> create(
            @Valid @RequestBody CreateDiscountCodeRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        discountCodeService
                                .createDiscountCode(request)
                );
    }

    @GetMapping
    public ResponseEntity<List<DiscountCodeResponse>> getAll() {

        return ResponseEntity.ok(
                discountCodeService
                        .getAllDiscountCodes()
        );
    }

    @PatchMapping("/{discountCodeId}/active")
    public ResponseEntity<DiscountCodeResponse> setActive(
            @PathVariable Long discountCodeId,
            @RequestParam boolean active
    ) {

        return ResponseEntity.ok(
                discountCodeService.setActive(
                        discountCodeId,
                        active
                )
        );
    }
}
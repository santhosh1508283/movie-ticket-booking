package com.dmg.movieticket.controller;

import com.dmg.movieticket.dto.request.CreatePaymentRequest;
import com.dmg.movieticket.dto.response.PaymentResponse;
import com.dmg.movieticket.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/bookings/{bookingId}")
    public ResponseEntity<PaymentResponse> processPayment(
            @PathVariable Long bookingId,
            @RequestHeader("Idempotency-Key")
            String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        return ResponseEntity.ok(
                paymentService.processPayment(
                        bookingId,
                        idempotencyKey,
                        request
                )
        );
    }
}
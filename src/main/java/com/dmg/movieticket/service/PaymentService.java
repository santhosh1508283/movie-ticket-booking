package com.dmg.movieticket.service;

import com.dmg.movieticket.dto.request.CreatePaymentRequest;
import com.dmg.movieticket.dto.response.PaymentResponse;

public interface PaymentService {

    PaymentResponse processPayment(
            Long bookingId,
            String idempotencyKey,
            CreatePaymentRequest request
    );
}
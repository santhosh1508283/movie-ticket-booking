package com.dmg.movieticket.service.payment;

import com.dmg.movieticket.entity.PaymentMethod;

import java.math.BigDecimal;

public interface PaymentProcessor {

    PaymentProcessingResult process(
            BigDecimal amount,
            PaymentMethod paymentMethod
    );
}
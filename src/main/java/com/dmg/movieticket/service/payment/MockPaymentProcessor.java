package com.dmg.movieticket.service.payment;

import com.dmg.movieticket.entity.PaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MockPaymentProcessor implements PaymentProcessor {

    @Override
    public PaymentProcessingResult process(
            BigDecimal amount,
            PaymentMethod paymentMethod
    ) {

        return new PaymentProcessingResult(
                true,
                "PAY-" + UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase(),
                null
        );
    }
}
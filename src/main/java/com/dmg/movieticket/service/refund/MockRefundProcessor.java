package com.dmg.movieticket.service.refund;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MockRefundProcessor implements RefundProcessor {

    @Override
    public RefundProcessingResult processRefund(
            String paymentReference,
            BigDecimal amount
    ) {

        return new RefundProcessingResult(
                true,
                "REF-" + UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase(),
                null
        );
    }
}
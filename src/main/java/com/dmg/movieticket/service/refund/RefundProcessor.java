package com.dmg.movieticket.service.refund;

import java.math.BigDecimal;

public interface RefundProcessor {

    RefundProcessingResult processRefund(
            String paymentReference,
            BigDecimal amount
    );
}
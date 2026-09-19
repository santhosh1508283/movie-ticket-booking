package com.dmg.movieticket.dto.request;

import com.dmg.movieticket.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod

) {
}
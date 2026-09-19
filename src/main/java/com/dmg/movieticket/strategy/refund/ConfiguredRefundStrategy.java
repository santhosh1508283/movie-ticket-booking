package com.dmg.movieticket.strategy.refund;

import com.dmg.movieticket.entity.Booking;
import com.dmg.movieticket.entity.RefundPolicy;
import com.dmg.movieticket.repository.RefundPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ConfiguredRefundStrategy
        implements RefundCalculationStrategy {

    private final RefundPolicyRepository refundPolicyRepository;

    @Override
    public RefundCalculationResult calculate(
            Booking booking,
            LocalDateTime cancellationTime
    ) {

        LocalDateTime showStartTime =
                booking.getShow().getStartTime();

        long hoursBeforeShow = Duration.between(
                cancellationTime,
                showStartTime
        ).toHours();

        List<RefundPolicy> policies =
                refundPolicyRepository
                        .findByActiveTrueOrderByHoursBeforeShowDesc();

        BigDecimal percentage = BigDecimal.ZERO;

        for (RefundPolicy policy : policies) {

            if (hoursBeforeShow >= policy.getHoursBeforeShow()) {
                percentage = policy.getRefundPercentage();
                break;
            }
        }

        BigDecimal amount = booking.getTotalAmount()
                .multiply(percentage)
                .divide(
                        new BigDecimal("100"),
                        2,
                        RoundingMode.HALF_UP
                );

        return new RefundCalculationResult(
                percentage,
                amount
        );
    }
}
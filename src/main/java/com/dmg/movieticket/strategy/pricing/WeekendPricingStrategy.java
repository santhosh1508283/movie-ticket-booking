package com.dmg.movieticket.strategy.pricing;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;

@Component
public class WeekendPricingStrategy implements PricingStrategy {

    private static final BigDecimal WEEKEND_MULTIPLIER =
            new BigDecimal("1.20");

    @Override
    public boolean supports(PricingContext context) {

        DayOfWeek day = context.showTime().getDayOfWeek();

        return day == DayOfWeek.SATURDAY
                || day == DayOfWeek.SUNDAY;
    }

    @Override
    public BigDecimal calculate(PricingContext context) {
        return context.basePrice()
                .multiply(WEEKEND_MULTIPLIER);
    }
}
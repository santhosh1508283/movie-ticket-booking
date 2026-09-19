package com.dmg.movieticket.strategy.pricing;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DefaultPricingStrategy implements PricingStrategy {

    @Override
    public boolean supports(PricingContext context) {
        return true;
    }

    @Override
    public BigDecimal calculate(PricingContext context) {
        return context.basePrice();
    }
}
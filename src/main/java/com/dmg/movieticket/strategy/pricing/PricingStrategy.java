package com.dmg.movieticket.strategy.pricing;

import java.math.BigDecimal;

public interface PricingStrategy {

    boolean supports(PricingContext context);

    BigDecimal calculate(PricingContext context);
}
package com.dmg.movieticket.resolver;

import com.dmg.movieticket.strategy.pricing.DefaultPricingStrategy;
import com.dmg.movieticket.strategy.pricing.PricingContext;
import com.dmg.movieticket.strategy.pricing.PricingStrategy;
import com.dmg.movieticket.strategy.pricing.WeekendPricingStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PricingStrategyResolver {

    private final WeekendPricingStrategy weekendPricingStrategy;
    private final DefaultPricingStrategy defaultPricingStrategy;

    public PricingStrategy resolve(PricingContext context) {

        if (weekendPricingStrategy.supports(context)) {
            return weekendPricingStrategy;
        }

        return defaultPricingStrategy;
    }
}
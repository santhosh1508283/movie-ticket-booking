package com.dmg.movieticket.strategy;

import com.dmg.movieticket.entity.SeatType;
import com.dmg.movieticket.resolver.PricingStrategyResolver;
import com.dmg.movieticket.strategy.pricing.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.LocalDateTime;
import static com.dmg.movieticket.support.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class PricingStrategiesTest {
    WeekendPricingStrategy weekend = new WeekendPricingStrategy();
    DefaultPricingStrategy standard = new DefaultPricingStrategy();
    PricingStrategyResolver resolver = new PricingStrategyResolver(weekend, standard);
    @ParameterizedTest
    @CsvSource({"2030-01-07T18:00,false,100", "2030-01-08T18:00,false,100",
            "2030-01-09T18:00,false,100", "2030-01-10T18:00,false,100",
            "2030-01-11T18:00,false,100", "2030-01-12T18:00,true,120", "2030-01-13T18:00,true,120"})
    void resolvesCorrectPriceForEveryDay(String date, boolean isWeekend, String expected) {
        for (SeatType type : SeatType.values()) {
            PricingContext context = new PricingContext(type, money("100"), LocalDateTime.parse(date));
            assertEquals(isWeekend, weekend.supports(context)); assertTrue(standard.supports(context));
            assertSame(isWeekend ? weekend : standard, resolver.resolve(context));
            assertEquals(0, money(expected).compareTo(resolver.resolve(context).calculate(context)));
        }
    }
}

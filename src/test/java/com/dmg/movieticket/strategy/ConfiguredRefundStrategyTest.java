package com.dmg.movieticket.strategy;

import com.dmg.movieticket.entity.*;
import com.dmg.movieticket.repository.RefundPolicyRepository;
import com.dmg.movieticket.strategy.refund.ConfiguredRefundStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.LocalDateTime;
import java.util.List;
import static com.dmg.movieticket.support.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfiguredRefundStrategyTest {
    RefundPolicyRepository repository = mock(RefundPolicyRepository.class);
    ConfiguredRefundStrategy strategy = new ConfiguredRefundStrategy(repository);
    LocalDateTime start = LocalDateTime.of(2030, 1, 10, 18, 0);
    @ParameterizedTest @CsvSource({"2880,100,100.05", "1440,100,100.05", "1439,50,50.03", "120,50,50.03", "119,0,0.00"})
    void usesMostGenerousEligibleThresholdWithRounding(int minutes, String percentage, String amount) {
        Booking booking = booking(); booking.getShow().setStartTime(start); booking.setTotalAmount(money("100.05"));
        when(repository.findByActiveTrueOrderByHoursBeforeShowDesc()).thenReturn(List.of(
                RefundPolicy.builder().hoursBeforeShow(24).refundPercentage(money("100")).build(),
                RefundPolicy.builder().hoursBeforeShow(2).refundPercentage(money("50")).build()));
        var result = strategy.calculate(booking, start.minusMinutes(minutes));
        assertEquals(0, money(percentage).compareTo(result.refundPercentage()));
        assertEquals(money(amount), result.refundAmount());
    }
    @Test void noApplicablePoliciesMeansZeroRefund() {
        Booking booking = booking(); booking.getShow().setStartTime(start);
        var result = strategy.calculate(booking, start.minusDays(1));
        assertEquals(0, result.refundAmount().signum()); assertEquals(0, result.refundPercentage().signum());
    }
}

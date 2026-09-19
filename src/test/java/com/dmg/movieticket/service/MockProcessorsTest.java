package com.dmg.movieticket.service;

import com.dmg.movieticket.entity.PaymentMethod;
import com.dmg.movieticket.service.payment.MockPaymentProcessor;
import com.dmg.movieticket.service.refund.MockRefundProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static com.dmg.movieticket.support.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class MockProcessorsTest {
    @ParameterizedTest @EnumSource(PaymentMethod.class)
    void mockPaymentSucceedsWithProviderReference(PaymentMethod method) {
        var result = new MockPaymentProcessor().process(money("100"), method);
        assertTrue(result.successful()); assertTrue(result.providerReference().matches("PAY-[0-9A-F]{8}"));
        assertNull(result.failureReason());
    }
    @Test void mockRefundSucceedsWithProviderReference() {
        var result = new MockRefundProcessor().processRefund("PAY-TEST", money("50"));
        assertTrue(result.successful()); assertTrue(result.providerReference().matches("REF-[0-9A-F]{8}"));
        assertNull(result.failureReason());
    }
}

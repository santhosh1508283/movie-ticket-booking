package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.entity.*;
import com.dmg.movieticket.repository.DiscountCodeRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.LocalDateTime;
import java.util.Optional;
import static com.dmg.movieticket.support.TestFixtures.*;
import static com.dmg.movieticket.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiscountServiceImplTest {
    DiscountCodeRepository repository = mock(DiscountCodeRepository.class);
    DiscountServiceImpl service = new DiscountServiceImpl(repository);
    DiscountCode discount;
    @BeforeEach void setUp() {
        discount = DiscountCode.builder().code("SAVE").discountType(DiscountType.FLAT).discountValue(money("25"))
                .active(true).validFrom(LocalDateTime.now().minusDays(1)).validUntil(LocalDateTime.now().plusDays(1)).build();
    }
    void found() { when(repository.findByCodeIgnoreCase("SAVE")).thenReturn(Optional.of(discount)); }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" ", "\t"})
    void noCodeMeansNoDiscount(String code) {
        var result = service.calculateDiscount(code, money("100"));
        assertNull(result.code()); assertEquals(0, result.discountAmount().signum()); verifyNoInteractions(repository);
    }
    @Test void flatDiscountNormalizesCode() {
        found(); assertEquals(money("25"), service.calculateDiscount(" save ", money("100")).discountAmount());
        verify(repository).findByCodeIgnoreCase("SAVE");
    }
    @Test void capsFlatDiscountAtSubtotal() {
        found(); assertEquals(money("10"), service.calculateDiscount("SAVE", money("10")).discountAmount());
    }
    @Test void percentageRoundsHalfUpToTwoDecimals() {
        found(); discount.setDiscountType(DiscountType.PERCENTAGE); discount.setDiscountValue(money("12.5"));
        assertEquals(money("12.51"), service.calculateDiscount("SAVE", money("100.04")).discountAmount());
    }
    @Test void percentageHonorsMaximumDiscount() {
        found(); discount.setDiscountType(DiscountType.PERCENTAGE); discount.setDiscountValue(money("50"));
        discount.setMaximumDiscountAmount(money("20"));
        assertEquals(money("20"), service.calculateDiscount("SAVE", money("100")).discountAmount());
    }
    @Test void acceptsExactMinimumOrder() {
        found(); discount.setMinimumOrderAmount(money("100"));
        assertEquals(money("25"), service.calculateDiscount("SAVE", money("100")).discountAmount());
    }
    @Test void rejectsBelowMinimum() {
        found(); discount.setMinimumOrderAmount(money("100"));
        error(INVALID_REQUEST, () -> service.calculateDiscount("SAVE", money("99.99")));
    }
    @Test void rejectsUnknownCode() { error(INVALID_REQUEST, () -> service.calculateDiscount("SAVE", money("100"))); }
    @Test void rejectsInactiveCode() {
        found(); discount.setActive(false); error(INVALID_REQUEST, () -> service.calculateDiscount("SAVE", money("100")));
    }
    @Test void rejectsFutureCode() {
        found(); discount.setValidFrom(LocalDateTime.now().plusDays(1));
        error(INVALID_REQUEST, () -> service.calculateDiscount("SAVE", money("100")));
    }
    @Test void rejectsExpiredCode() {
        found(); discount.setValidUntil(LocalDateTime.now().minusDays(1));
        error(INVALID_REQUEST, () -> service.calculateDiscount("SAVE", money("100")));
    }
}

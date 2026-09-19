package com.dmg.movieticket.dto;

import com.dmg.movieticket.dto.request.*;
import com.dmg.movieticket.entity.*;
import jakarta.validation.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;
import static com.dmg.movieticket.support.TestFixtures.money;
import static org.junit.jupiter.api.Assertions.*;

class RequestValidationTest {
    static ValidatorFactory factory;
    static Validator validator;
    static final LocalDateTime START = LocalDateTime.of(2030, 1, 1, 18, 0);
    @BeforeAll static void startValidator() { factory = Validation.buildDefaultValidatorFactory(); validator = factory.getValidator(); }
    @AfterAll static void closeValidator() { factory.close(); }
    static Stream<Object> validRequests() {
        return Stream.of(
                new CreateCityRequest("Bengaluru"),
                new CreateMovieRequest("Movie", null, 1, "English", "Drama"),
                new CreateTheaterRequest("PVR", "Road", null, null, 1L),
                new CreateScreenRequest("Screen 1", 1L),
                new CreateSeatRequest("A", 1, SeatType.REGULAR),
                new CreateSeatLayoutRequest(1L, List.of(new CreateSeatRequest("B", 2, SeatType.PREMIUM))),
                new CreateShowRequest(1L, 1L, START, money("0.01"), money("0.01")),
                new CreateSeatHoldRequest(1L, List.of(1L)),
                new CreateBookingRequest(1L, null),
                new CreatePaymentRequest(PaymentMethod.UPI),
                new CreateDiscountCodeRequest("SAVE", DiscountType.FLAT, money("0.01"), null, null, START, START.plusDays(1)),
                new CreateRefundPolicyRequest(0, money("0.00")),
                new CreateRefundPolicyRequest(24, money("100.00")),
                new RegisterRequest("Customer", "customer@example.test", "password123"),
                new LoginRequest("customer@example.test", "password123"),
                new RefreshTokenRequest("token"),
                new UpdateUserRoleRequest("customer@example.test"));
    }
    @ParameterizedTest @MethodSource("validRequests")
    void acceptsValidRequests(Object request) { assertTrue(validator.validate(request).isEmpty()); }
    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of(new CreateCityRequest(" "), "name"),
                Arguments.of(new CreateCityRequest("x".repeat(101)), "name"),
                Arguments.of(new CreateMovieRequest("Movie", null, 0, "English", "Drama"), "durationMinutes"),
                Arguments.of(new CreateMovieRequest("x".repeat(151), null, 1, "English", "Drama"), "title"),
                Arguments.of(new CreateMovieRequest("Movie", "x".repeat(1001), 1, "English", "Drama"), "description"),
                Arguments.of(new CreateMovieRequest("Movie", null, 1, "", "Drama"), "language"),
                Arguments.of(new CreateMovieRequest("Movie", null, 1, "English", ""), "genre"),
                Arguments.of(new CreateTheaterRequest("PVR", " ", null, null, 1L), "address"),
                Arguments.of(new CreateTheaterRequest("PVR", "Road", null, null, 0L), "cityId"),
                Arguments.of(new CreateTheaterRequest("PVR", "Road", "x".repeat(101), null, 1L), "landmark"),
                Arguments.of(new CreateTheaterRequest("PVR", "Road", null, "x".repeat(21), 1L), "postalCode"),
                Arguments.of(new CreateScreenRequest("Screen", -1L), "theaterId"),
                Arguments.of(new CreateScreenRequest("x".repeat(101), 1L), "name"),
                Arguments.of(new CreateSeatRequest("", 1, SeatType.REGULAR), "rowLabel"),
                Arguments.of(new CreateSeatRequest("A", 0, SeatType.REGULAR), "seatNumber"),
                Arguments.of(new CreateSeatRequest("A", 1, null), "seatType"),
                Arguments.of(new CreateSeatLayoutRequest(1L, List.of()), "seats"),
                Arguments.of(new CreateSeatLayoutRequest(1L, List.of(new CreateSeatRequest("A", 0, SeatType.REGULAR))), "seats[0].seatNumber"),
                Arguments.of(new CreateShowRequest(1L, 1L, START, money("0"), money("1")), "regularBasePrice"),
                Arguments.of(new CreateShowRequest(1L, 1L, START, money("1"), money("-1")), "premiumBasePrice"),
                Arguments.of(new CreateSeatHoldRequest(1L, List.of()), "showSeatIds"),
                Arguments.of(new CreateSeatHoldRequest(1L, Arrays.asList((Long) null)), "showSeatIds[0].<list element>"),
                Arguments.of(new CreateBookingRequest(null, null), "holdId"),
                Arguments.of(new CreateBookingRequest(1L, "x".repeat(51)), "discountCode"),
                Arguments.of(new CreatePaymentRequest(null), "paymentMethod"),
                Arguments.of(new CreateDiscountCodeRequest("SAVE", DiscountType.FLAT, money("0"), null, null, START, START.plusDays(1)), "discountValue"),
                Arguments.of(new CreateDiscountCodeRequest("SAVE", DiscountType.FLAT, money("1"), money("-1"), null, START, START.plusDays(1)), "minimumOrderAmount"),
                Arguments.of(new CreateDiscountCodeRequest("SAVE", DiscountType.FLAT, money("1"), null, money("-1"), START, START.plusDays(1)), "maximumDiscountAmount"),
                Arguments.of(new CreateRefundPolicyRequest(-1, money("50")), "hoursBeforeShow"),
                Arguments.of(new CreateRefundPolicyRequest(0, money("-0.01")), "refundPercentage"),
                Arguments.of(new CreateRefundPolicyRequest(0, money("100.01")), "refundPercentage"),
                Arguments.of(new RegisterRequest("Customer", "not-email", "password123"), "email"),
                Arguments.of(new RegisterRequest("Customer", "customer@example.test", "short"), "password"),
                Arguments.of(new RegisterRequest("Customer", "customer@example.test", "x".repeat(101)), "password"),
                Arguments.of(new LoginRequest("not-email", "password123"), "email"),
                Arguments.of(new LoginRequest("customer@example.test", ""), "password"),
                Arguments.of(new RefreshTokenRequest(" "), "refreshToken"),
                Arguments.of(new UpdateUserRoleRequest("not-email"), "email"));
    }
    @ParameterizedTest @MethodSource("invalidRequests")
    void rejectsInvalidField(Object request, String field) {
        assertTrue(validator.validate(request).stream().anyMatch(v -> v.getPropertyPath().toString().equals(field)),
                () -> "Expected validation error for " + field + " in " + request.getClass().getSimpleName());
    }
}

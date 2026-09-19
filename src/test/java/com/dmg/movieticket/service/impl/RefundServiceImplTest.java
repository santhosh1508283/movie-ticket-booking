package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.entity.*;
import com.dmg.movieticket.event.*;
import com.dmg.movieticket.mapper.RefundMapper;
import com.dmg.movieticket.repository.*;
import com.dmg.movieticket.security.CurrentUserService;
import com.dmg.movieticket.service.refund.*;
import com.dmg.movieticket.strategy.refund.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mapstruct.factory.Mappers;
import org.springframework.context.ApplicationEventPublisher;
import java.time.LocalDateTime;
import java.util.*;
import static com.dmg.movieticket.support.TestFixtures.*;
import static com.dmg.movieticket.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefundServiceImplTest {
    BookingRepository bookings = mock(BookingRepository.class);
    PaymentRepository payments = mock(PaymentRepository.class);
    RefundRepository refunds = mock(RefundRepository.class);
    ShowSeatRepository seats = mock(ShowSeatRepository.class);
    RefundCalculationStrategy calculation = mock(RefundCalculationStrategy.class);
    RefundProcessor processor = mock(RefundProcessor.class);
    CurrentUserService current = mock(CurrentUserService.class);
    ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    RefundServiceImpl service = new RefundServiceImpl(bookings, payments, refunds, seats, calculation,
            processor, Mappers.getMapper(RefundMapper.class), current, events);
    Booking booking; Payment payment;
    @BeforeEach void setUp() {
        booking = booking(); booking.setStatus(BookingStatus.CONFIRMED);
        booking.getItems().get(0).getShowSeat().setStatus(ShowSeatStatus.BOOKED);
        payment = Payment.builder().id(9L).booking(booking).status(PaymentStatus.SUCCESS).providerReference("PAY").build();
        when(current.getCurrentUser()).thenReturn(user());
    }
    void ownedBooking() { when(bookings.findByIdAndUserIdForUpdate(8L, 1L)).thenReturn(Optional.of(booking)); }
    void cancellable(String amount) {
        ownedBooking();
        when(payments.findFirstByBookingIdAndStatusOrderByCreatedAtDesc(8L, PaymentStatus.SUCCESS)).thenReturn(Optional.of(payment));
        when(calculation.calculate(eq(booking), any())).thenReturn(new RefundCalculationResult(money("50"), money(amount)));
        when(seats.findByIdsForUpdate(List.of(10L))).thenReturn(List.of(booking.getItems().get(0).getShowSeat()));
        when(refunds.save(any())).thenAnswer(i -> { Refund r = i.getArgument(0); r.setId(11L); return r; });
    }
    @Test void successfulRefundCancelsBookingAndReleasesSeats() {
        cancellable("75.00");
        when(processor.processRefund("PAY", money("75.00"))).thenReturn(new RefundProcessingResult(true, "REF", null));
        var result = service.cancelBooking(8L);
        assertEquals(BookingStatus.CANCELLED, booking.getStatus()); assertNotNull(booking.getCancelledAt());
        assertEquals(ShowSeatStatus.AVAILABLE, booking.getItems().get(0).getShowSeat().getStatus());
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        assertEquals(RefundStatus.SUCCESS, result.refund().status()); assertEquals(money("75.00"), result.refund().amount());
        verify(events).publishEvent(new BookingCancelledEvent(8L)); verify(events).publishEvent(new RefundProcessedEvent(11L));
    }
    @Test void zeroRefundCancelsWithoutCallingProviderOrMarkingPaymentRefunded() {
        cancellable("0");
        var result = service.cancelBooking(8L);
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(RefundStatus.SUCCESS, result.refund().status());
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus()); verifyNoInteractions(processor);
    }
    @Test void providerFailureDoesNotCancelOrReleaseSeats() {
        cancellable("75.00");
        when(processor.processRefund(any(), any())).thenReturn(new RefundProcessingResult(false, null, "Unavailable"));
        error(INVALID_STATE, () -> service.cancelBooking(8L));
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(ShowSeatStatus.BOOKED, booking.getItems().get(0).getShowSeat().getStatus());
        verifyNoInteractions(events); verify(bookings, never()).save(any());
    }
    @Test void rejectsMissingOrUnownedBooking() {
        error(RESOURCE_NOT_FOUND, () -> service.cancelBooking(8L)); verifyNoInteractions(processor);
    }
    @ParameterizedTest @EnumSource(value = BookingStatus.class, names = "CONFIRMED", mode = EnumSource.Mode.EXCLUDE)
    void rejectsUnconfirmedBooking(BookingStatus state) {
        ownedBooking(); booking.setStatus(state);
        error(CANCELLATION_NOT_ALLOWED, () -> service.cancelBooking(8L)); verifyNoInteractions(processor);
    }
    @Test void rejectsStartedShow() {
        ownedBooking(); booking.getShow().setStartTime(LocalDateTime.now().minusMinutes(1));
        error(CANCELLATION_NOT_ALLOWED, () -> service.cancelBooking(8L)); verifyNoInteractions(processor);
    }
    @Test void rejectsExistingRefund() {
        ownedBooking(); when(refunds.existsByBookingId(8L)).thenReturn(true);
        error(INVALID_STATE, () -> service.cancelBooking(8L)); verifyNoInteractions(processor);
    }
    @Test void requiresSuccessfulPayment() {
        ownedBooking(); error(INVALID_STATE, () -> service.cancelBooking(8L)); verifyNoInteractions(processor);
    }
    @Test void rejectsMissingSeat() {
        cancellable("75"); when(seats.findByIdsForUpdate(List.of(10L))).thenReturn(List.of());
        error(INVALID_STATE, () -> service.cancelBooking(8L)); verifyNoInteractions(processor);
    }
    @ParameterizedTest @EnumSource(value = ShowSeatStatus.class, names = "BOOKED", mode = EnumSource.Mode.EXCLUDE)
    void rejectsInvalidSeatState(ShowSeatStatus state) {
        cancellable("75"); booking.getItems().get(0).getShowSeat().setStatus(state);
        error(INVALID_STATE, () -> service.cancelBooking(8L)); verifyNoInteractions(processor);
    }
}

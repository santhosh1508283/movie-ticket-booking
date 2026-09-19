package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.CreatePaymentRequest;
import com.dmg.movieticket.entity.*;
import com.dmg.movieticket.event.BookingConfirmedEvent;
import com.dmg.movieticket.mapper.PaymentMapper;
import com.dmg.movieticket.repository.*;
import com.dmg.movieticket.security.CurrentUserService;
import com.dmg.movieticket.service.payment.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mapstruct.factory.Mappers;
import org.springframework.context.ApplicationEventPublisher;
import java.time.LocalDateTime;
import java.util.*;
import static com.dmg.movieticket.support.TestFixtures.*;
import static com.dmg.movieticket.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceImplTest {
    BookingRepository bookings = mock(BookingRepository.class);
    PaymentRepository payments = mock(PaymentRepository.class);
    ShowSeatRepository seats = mock(ShowSeatRepository.class);
    PaymentProcessor processor = mock(PaymentProcessor.class);
    CurrentUserService current = mock(CurrentUserService.class);
    ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    PaymentServiceImpl service = new PaymentServiceImpl(bookings, payments, seats, processor,
            Mappers.getMapper(PaymentMapper.class), current, events);
    Booking booking;
    CreatePaymentRequest request = new CreatePaymentRequest(PaymentMethod.CARD);
    @BeforeEach void setUp() { booking = booking(); when(current.getCurrentUser()).thenReturn(user()); }
    void ownedBooking() { when(bookings.findByIdAndUserIdForUpdate(8L, 1L)).thenReturn(Optional.of(booking)); }
    void payable() {
        ownedBooking();
        when(seats.findByIdsForUpdate(List.of(10L))).thenReturn(List.of(booking.getItems().get(0).getShowSeat()));
        when(payments.save(any())).thenAnswer(i -> { Payment p = i.getArgument(0); p.setId(9L); return p; });
    }
    @Test void confirmsBookingHoldAndSeatsOnSuccess() {
        payable();
        when(processor.process(money("150.00"), PaymentMethod.CARD))
                .thenReturn(new PaymentProcessingResult(true, "PAY-TEST", null));
        var result = service.processPayment(8L, " key ", request);
        assertEquals(PaymentStatus.SUCCESS, result.status()); assertEquals("PAY-TEST", result.providerReference());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus()); assertNotNull(booking.getConfirmedAt());
        assertEquals(HoldStatus.CONFIRMED, booking.getSeatHold().getStatus());
        assertEquals(ShowSeatStatus.BOOKED, booking.getItems().get(0).getShowSeat().getStatus());
        verify(payments).findByIdempotencyKey("key"); verify(events).publishEvent(new BookingConfirmedEvent(8L));
        verify(bookings).save(booking);
    }
    @Test void failedPaymentLeavesReservationRetryable() {
        payable();
        when(processor.process(any(), any())).thenReturn(new PaymentProcessingResult(false, null, "Declined"));
        assertEquals(PaymentStatus.FAILED, service.processPayment(8L, "key", request).status());
        assertEquals(BookingStatus.PENDING_PAYMENT, booking.getStatus());
        assertEquals(HoldStatus.ACTIVE, booking.getSeatHold().getStatus());
        assertEquals(ShowSeatStatus.HELD, booking.getItems().get(0).getShowSeat().getStatus());
        verifyNoInteractions(events); verify(bookings, never()).save(any()); verify(seats, never()).saveAll(any());
    }
    @Test void retryWithNewKeyCanSucceedAfterDecline() {
        payable();
        when(processor.process(any(), any()))
                .thenReturn(new PaymentProcessingResult(false, null, "Declined"))
                .thenReturn(new PaymentProcessingResult(true, "PAY-RETRY", null));
        assertEquals(PaymentStatus.FAILED, service.processPayment(8L, "first", request).status());
        assertEquals(PaymentStatus.SUCCESS, service.processPayment(8L, "second", request).status());
        verify(events, times(1)).publishEvent(new BookingConfirmedEvent(8L));
    }
    @Test void idempotentRetryReturnsStoredResultWithoutProcessing() {
        Payment existing = Payment.builder().id(9L).booking(booking).status(PaymentStatus.SUCCESS).build();
        when(payments.findByIdempotencyKey("key")).thenReturn(Optional.of(existing));
        assertEquals(9L, service.processPayment(8L, " key ", request).paymentId());
        verifyNoInteractions(processor, seats, events, bookings);
    }
    @Test void rejectsKeyOwnedByAnotherUser() {
        booking.setUser(User.builder().id(2L).build());
        when(payments.findByIdempotencyKey("key")).thenReturn(Optional.of(Payment.builder().booking(booking).build()));
        error(INVALID_REQUEST, () -> service.processPayment(8L, "key", request)); verifyNoInteractions(processor);
    }
    @Test void rejectsKeyForAnotherBooking() {
        when(payments.findByIdempotencyKey("key")).thenReturn(Optional.of(Payment.builder().booking(booking).build()));
        error(INVALID_REQUEST, () -> service.processPayment(99L, "key", request)); verifyNoInteractions(processor);
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" ", "\t"})
    void requiresIdempotencyKey(String key) {
        error(INVALID_REQUEST, () -> service.processPayment(8L, key, request)); verifyNoInteractions(payments, processor);
    }
    @Test void rejectsMissingOrUnownedBooking() {
        error(RESOURCE_NOT_FOUND, () -> service.processPayment(8L, "key", request));
        verify(bookings).findByIdAndUserIdForUpdate(8L, 1L); verifyNoInteractions(processor);
    }
    @ParameterizedTest @EnumSource(value = BookingStatus.class, names = "PENDING_PAYMENT", mode = EnumSource.Mode.EXCLUDE)
    void rejectsIneligibleBookingStates(BookingStatus state) {
        ownedBooking(); booking.setStatus(state);
        error(state == BookingStatus.EXPIRED ? HOLD_EXPIRED : INVALID_STATE,
                () -> service.processPayment(8L, "key", request)); verifyNoInteractions(processor);
    }
    @Test void rejectsExpiredHoldWithoutChangingState() {
        ownedBooking(); booking.getSeatHold().setExpiresAt(LocalDateTime.now().minusMinutes(1));
        error(HOLD_EXPIRED, () -> service.processPayment(8L, "key", request));
        assertEquals(BookingStatus.PENDING_PAYMENT, booking.getStatus()); verifyNoInteractions(processor);
    }
    @Test void rejectsInactiveHold() {
        ownedBooking(); booking.getSeatHold().setStatus(HoldStatus.CANCELLED);
        error(HOLD_EXPIRED, () -> service.processPayment(8L, "key", request)); verifyNoInteractions(processor);
    }
    @Test void rejectsBookingWithoutSeats() {
        ownedBooking(); booking.getItems().clear();
        error(INVALID_STATE, () -> service.processPayment(8L, "key", request)); verifyNoInteractions(processor);
    }
    @Test void rejectsMissingLockedSeat() {
        ownedBooking(); error(INVALID_STATE, () -> service.processPayment(8L, "key", request));
        verifyNoInteractions(processor);
    }
    @ParameterizedTest @EnumSource(value = ShowSeatStatus.class, names = "HELD", mode = EnumSource.Mode.EXCLUDE)
    void rejectsSeatsNoLongerHeld(ShowSeatStatus state) {
        payable(); booking.getItems().get(0).getShowSeat().setStatus(state);
        error(INVALID_STATE, () -> service.processPayment(8L, "key", request)); verifyNoInteractions(processor, events);
    }
}

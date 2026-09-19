package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.CreateBookingRequest;
import com.dmg.movieticket.dto.response.DiscountResult;
import com.dmg.movieticket.entity.*;
import com.dmg.movieticket.mapper.BookingMapper;
import com.dmg.movieticket.repository.*;
import com.dmg.movieticket.security.CurrentUserService;
import com.dmg.movieticket.service.DiscountService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import java.time.LocalDateTime;
import java.util.*;
import static com.dmg.movieticket.support.TestFixtures.*;
import static com.dmg.movieticket.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingServiceImplTest {
    BookingRepository bookings = mock(BookingRepository.class);
    SeatHoldRepository holds = mock(SeatHoldRepository.class);
    DiscountService discounts = mock(DiscountService.class);
    CurrentUserService current = mock(CurrentUserService.class);
    BookingServiceImpl service = new BookingServiceImpl(bookings, holds, discounts,
            Mappers.getMapper(BookingMapper.class), current);
    SeatHold hold;
    @BeforeEach void setUp() { hold = hold(); when(current.getCurrentUser()).thenReturn(user()); }
    void ownedHold() { when(holds.findByIdAndUserIdForUpdate(7L, 1L)).thenReturn(Optional.of(hold)); }
    CreateBookingRequest request() { return new CreateBookingRequest(7L, "SAVE"); }
    @Test void snapshotsHeldPricesAndSeatDetailsWithDiscount() {
        ownedHold();
        hold.getItems().get(0).getShowSeat().setPrice(money("999"));
        when(discounts.calculateDiscount("SAVE", money("150.00"))).thenReturn(new DiscountResult("SAVE", money("25.00")));
        when(bookings.save(any())).thenAnswer(i -> { Booking b = i.getArgument(0); b.setId(8L); return b; });
        var result = service.createBooking(request());
        assertEquals(money("125.00"), result.totalAmount());
        assertEquals(money("150.00"), result.subtotal());
        assertEquals(BookingStatus.PENDING_PAYMENT, result.status());
        assertEquals("A10", result.items().get(0).seatLabel());
        assertEquals(money("150.00"), result.items().get(0).price());
        assertTrue(result.bookingReference().matches("DMG-[0-9A-F]{8}"));
        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookings).save(saved.capture());
        assertSame(saved.getValue(), saved.getValue().getItems().get(0).getBooking());
        assertSame(hold, saved.getValue().getSeatHold());
        assertEquals(HoldStatus.ACTIVE, hold.getStatus());
    }
    @Test void rejectsHoldNotOwnedByCurrentUser() {
        error(RESOURCE_NOT_FOUND, () -> service.createBooking(request()));
        verify(holds).findByIdAndUserIdForUpdate(7L, 1L); verifyNoInteractions(discounts);
    }
    @ParameterizedTest @EnumSource(value = HoldStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void rejectsInactiveHolds(HoldStatus status) {
        ownedHold(); hold.setStatus(status);
        error(INVALID_STATE, () -> service.createBooking(request())); verify(bookings, never()).save(any());
    }
    @Test void rejectsExpiredHold() {
        ownedHold(); hold.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        error(HOLD_EXPIRED, () -> service.createBooking(request())); verifyNoInteractions(discounts);
    }
    @Test void rejectsEmptyHold() {
        ownedHold(); hold.getItems().clear(); error(INVALID_STATE, () -> service.createBooking(request()));
        verify(bookings, never()).save(any());
    }
    @Test void rejectsRepeatedConversion() {
        ownedHold(); when(bookings.existsBySeatHoldId(7L)).thenReturn(true);
        error(INVALID_STATE, () -> service.createBooking(request())); verifyNoInteractions(discounts);
    }
    @Test void getsOnlyOwnedBooking() {
        when(bookings.findByIdAndUserId(8L, 1L)).thenReturn(Optional.of(booking()));
        assertEquals(8L, service.getBooking(8L).id());
        verify(bookings).findByIdAndUserId(8L, 1L);
    }
    @Test void hidesMissingOrUnownedBooking() { error(RESOURCE_NOT_FOUND, () -> service.getBooking(99L)); }
    @Test void listsOnlyCurrentUsersBookings() {
        when(bookings.findByUserId(1L)).thenReturn(List.of(booking()));
        assertEquals(1, service.getMyBookings().size()); verify(bookings).findByUserId(1L);
    }
    @Test void returnsEmptyHistory() { assertTrue(service.getMyBookings().isEmpty()); }
}

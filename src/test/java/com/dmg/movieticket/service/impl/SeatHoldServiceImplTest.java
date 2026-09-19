package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.CreateSeatHoldRequest;
import com.dmg.movieticket.entity.*;
import com.dmg.movieticket.mapper.SeatHoldMapper;
import com.dmg.movieticket.repository.*;
import com.dmg.movieticket.security.CurrentUserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.*;
import static com.dmg.movieticket.support.TestFixtures.*;
import static com.dmg.movieticket.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SeatHoldServiceImplTest {
    ShowRepository shows = mock(ShowRepository.class);
    ShowSeatRepository seats = mock(ShowSeatRepository.class);
    SeatHoldRepository holds = mock(SeatHoldRepository.class);
    SeatHoldItemRepository items = mock(SeatHoldItemRepository.class);
    BookingRepository bookings = mock(BookingRepository.class);
    CurrentUserService current = mock(CurrentUserService.class);
    SeatHoldServiceImpl service = new SeatHoldServiceImpl(shows, seats, holds, items,
            Mappers.getMapper(SeatHoldMapper.class), current, bookings);
    Show show; SeatHold hold;
    @BeforeEach void setUp() {
        show = show(); hold = hold();
        when(current.getCurrentUser()).thenReturn(user());
        ReflectionTestUtils.setField(service, "holdDurationMinutes", 5L);
    }
    void scheduledShow() { when(shows.findById(5L)).thenReturn(Optional.of(show)); }
    void ownedHold() { when(holds.findByIdAndUserId(7L, 1L)).thenReturn(Optional.of(hold)); }
    void releasableHold() {
        ownedHold(); when(items.findBySeatHoldId(7L)).thenReturn(hold.getItems());
        when(seats.findByIdsForUpdate(List.of(10L))).thenReturn(List.of(hold.getItems().get(0).getShowSeat()));
    }
    @Test void locksSortedSeatsAndSnapshotsPricesForConfiguredDuration() {
        scheduledShow();
        ShowSeat first = showSeat(10L, ShowSeatStatus.AVAILABLE), second = showSeat(11L, ShowSeatStatus.AVAILABLE);
        when(seats.findByShowIdAndIdsForUpdate(5L, List.of(10L, 11L))).thenReturn(List.of(first, second));
        when(holds.save(any())).thenAnswer(i -> {
            SeatHold saved = i.getArgument(0); saved.setId(7L);
            assertTrue(saved.getItems().stream().allMatch(item -> item.getSeatHold() == saved));
            return saved;
        });
        LocalDateTime before = LocalDateTime.now();
        var result = service.createHold(new CreateSeatHoldRequest(5L, List.of(11L, 10L)));
        assertEquals(HoldStatus.ACTIVE, result.status()); assertEquals(money("300.00"), result.totalAmount());
        assertEquals(2, result.seats().size()); assertEquals("A10", result.seats().get(0).seatLabel());
        assertFalse(result.expiresAt().isBefore(before.plusMinutes(5)));
        assertFalse(result.expiresAt().isAfter(LocalDateTime.now().plusMinutes(5)));
        assertEquals(ShowSeatStatus.HELD, first.getStatus()); assertEquals(ShowSeatStatus.HELD, second.getStatus());
        verify(seats).findByShowIdAndIdsForUpdate(5L, List.of(10L, 11L));
    }
    @Test void rejectsDuplicateSeatIdsBeforeLocking() {
        scheduledShow();
        error(INVALID_REQUEST, () -> service.createHold(new CreateSeatHoldRequest(5L, List.of(10L, 10L))));
        verifyNoInteractions(seats);
    }
    @Test void rejectsMissingShow() {
        error(RESOURCE_NOT_FOUND, () -> service.createHold(new CreateSeatHoldRequest(5L, List.of(10L))));
    }
    @ParameterizedTest @EnumSource(value = ShowStatus.class, names = "SCHEDULED", mode = EnumSource.Mode.EXCLUDE)
    void rejectsUnscheduledShows(ShowStatus status) {
        scheduledShow(); show.setStatus(status);
        error(INVALID_STATE, () -> service.createHold(new CreateSeatHoldRequest(5L, List.of(10L))));
        verifyNoInteractions(seats);
    }
    @Test void rejectsStartedShow() {
        scheduledShow(); show.setStartTime(LocalDateTime.now().minusMinutes(1));
        error(INVALID_STATE, () -> service.createHold(new CreateSeatHoldRequest(5L, List.of(10L))));
    }
    @Test void rejectsSeatsOutsideShow() {
        scheduledShow();
        error(RESOURCE_NOT_FOUND, () -> service.createHold(new CreateSeatHoldRequest(5L, List.of(99L))));
        verify(holds, never()).save(any());
    }
    @Test void bookedSeatRejectsWholeSelectionWithoutHoldingAvailableSeat() {
        scheduledShow(); ShowSeat available = showSeat(10L, ShowSeatStatus.AVAILABLE);
        when(seats.findByShowIdAndIdsForUpdate(5L, List.of(10L, 11L)))
                .thenReturn(List.of(available, showSeat(11L, ShowSeatStatus.BOOKED)));
        error(SEAT_NOT_AVAILABLE, () -> service.createHold(new CreateSeatHoldRequest(5L, List.of(10L, 11L))));
        assertEquals(ShowSeatStatus.AVAILABLE, available.getStatus()); verify(holds, never()).save(any());
    }
    @Test void rejectsSeatHeldByAnotherValidHold() {
        scheduledShow();
        when(seats.findByShowIdAndIdsForUpdate(5L, List.of(10L))).thenReturn(List.of(hold.getItems().get(0).getShowSeat()));
        when(items.findFirstByShowSeatIdAndSeatHoldStatusOrderBySeatHoldExpiresAtDesc(10L, HoldStatus.ACTIVE))
                .thenReturn(Optional.of(hold.getItems().get(0)));
        error(SEAT_NOT_AVAILABLE, () -> service.createHold(new CreateSeatHoldRequest(5L, List.of(10L))));
        verify(holds, never()).save(any());
    }
    @Test void rejectsHeldSeatWithoutHoldRecord() {
        scheduledShow();
        when(seats.findByShowIdAndIdsForUpdate(5L, List.of(10L))).thenReturn(List.of(showSeat(10L, ShowSeatStatus.HELD)));
        error(INVALID_STATE, () -> service.createHold(new CreateSeatHoldRequest(5L, List.of(10L))));
    }
    @Test void reclaimsExpiredHoldSynchronously() {
        scheduledShow(); releasableHold(); hold.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(seats.findByShowIdAndIdsForUpdate(5L, List.of(10L))).thenReturn(List.of(hold.getItems().get(0).getShowSeat()));
        when(items.findFirstByShowSeatIdAndSeatHoldStatusOrderBySeatHoldExpiresAtDesc(10L, HoldStatus.ACTIVE))
                .thenReturn(Optional.of(hold.getItems().get(0)));
        when(holds.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals(HoldStatus.ACTIVE, service.createHold(new CreateSeatHoldRequest(5L, List.of(10L))).status());
        assertEquals(HoldStatus.EXPIRED, hold.getStatus());
    }
    @Test void readsOwnedActiveHold() {
        ownedHold(); assertEquals(7L, service.getHold(7L).holdId());
        verify(holds).findByIdAndUserId(7L, 1L);
    }
    @Test void hidesMissingOrUnownedHoldOnReadAndRelease() {
        error(RESOURCE_NOT_FOUND, () -> service.getHold(7L));
        error(RESOURCE_NOT_FOUND, () -> service.releaseHold(7L));
    }
    @Test void readingExpiredHoldExpiresPendingBookingAndReleasesSeats() {
        releasableHold(); hold.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        Booking booking = booking(); when(bookings.findBySeatHoldId(7L)).thenReturn(Optional.of(booking));
        assertEquals(HoldStatus.EXPIRED, service.getHold(7L).status());
        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
        assertEquals(ShowSeatStatus.AVAILABLE, hold.getItems().get(0).getShowSeat().getStatus());
    }
    @Test void releaseCancelsActiveHold() {
        releasableHold(); service.releaseHold(7L);
        assertEquals(HoldStatus.CANCELLED, hold.getStatus());
        assertEquals(ShowSeatStatus.AVAILABLE, hold.getItems().get(0).getShowSeat().getStatus());
    }
    @ParameterizedTest @EnumSource(value = HoldStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void rejectsReleaseOfInactiveHold(HoldStatus status) {
        ownedHold(); hold.setStatus(status); error(INVALID_STATE, () -> service.releaseHold(7L));
        verifyNoInteractions(seats);
    }
    @Test void expiredReleaseReportsExpiry() {
        releasableHold(); hold.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        error(HOLD_EXPIRED, () -> service.releaseHold(7L));
        // This unit test checks the error only; transaction rollback requires a database test.
    }
    @Test void cleanupPreservesBookedSeatsAndConfirmedBookings() {
        hold.setExpiresAt(LocalDateTime.now().minusMinutes(1)); releasableHold();
        hold.getItems().get(0).getShowSeat().setStatus(ShowSeatStatus.BOOKED);
        Booking booking = booking(); booking.setStatus(BookingStatus.CONFIRMED);
        when(bookings.findBySeatHoldId(7L)).thenReturn(Optional.of(booking));
        when(holds.findByStatusAndExpiresAtBefore(eq(HoldStatus.ACTIVE), any())).thenReturn(List.of(hold));
        service.expireExpiredHolds();
        assertEquals(HoldStatus.EXPIRED, hold.getStatus());
        assertEquals(ShowSeatStatus.BOOKED, hold.getItems().get(0).getShowSeat().getStatus());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus()); verify(bookings, never()).save(any());
    }
    @Test void cleanupWithNoExpiredHoldsDoesNothing() {
        service.expireExpiredHolds(); verifyNoInteractions(seats, items, bookings);
    }
}

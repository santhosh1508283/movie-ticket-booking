package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.*;
import com.dmg.movieticket.entity.*;
import com.dmg.movieticket.mapper.SeatMapper;
import com.dmg.movieticket.repository.*;
import org.junit.jupiter.api.*;
import org.mapstruct.factory.Mappers;
import java.util.*;
import static com.dmg.movieticket.support.TestFixtures.*;
import static com.dmg.movieticket.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SeatServiceImplTest {
    SeatRepository seats;
    ScreenRepository screens;
    SeatServiceImpl service;
    @BeforeEach void setUp() {
        seats = mock(SeatRepository.class); screens = mock(ScreenRepository.class);
        service = new SeatServiceImpl(seats, screens, Mappers.getMapper(SeatMapper.class));
    }
    @Test void createsMixedLayoutAndNormalizesRows() {
        Screen screen = screen();
        when(screens.findById(4L)).thenReturn(Optional.of(screen));
        when(seats.saveAll(anyList())).thenAnswer(i -> {
            List<Seat> saved = i.getArgument(0);
            for (int n = 0; n < saved.size(); n++) {
                assertSame(screen, saved.get(n).getScreen());
                assertTrue(saved.get(n).getActive());
                saved.get(n).setId((long) n + 1);
            }
            return saved;
        });
        var result = service.createSeatLayout(new CreateSeatLayoutRequest(4L, List.of(
                new CreateSeatRequest(" a ", 1, SeatType.REGULAR),
                new CreateSeatRequest("b", 1, SeatType.PREMIUM))));
        assertEquals(2, result.size());
        assertEquals("A", result.get(0).rowLabel());
        assertEquals("B", result.get(1).rowLabel());
        assertEquals(SeatType.PREMIUM, result.get(1).seatType());
        assertEquals("Screen 1", result.get(0).screenName());
        verify(seats).existsByScreenIdAndRowLabelIgnoreCaseAndSeatNumber(4L, "A", 1);
        verify(seats).existsByScreenIdAndRowLabelIgnoreCaseAndSeatNumber(4L, "B", 1);
    }
    @Test void rejectsDuplicatesWithinRequestBeforeSavingAnySeats() {
        when(screens.findById(4L)).thenReturn(Optional.of(screen()));
        error(DUPLICATE_RESOURCE, () -> service.createSeatLayout(new CreateSeatLayoutRequest(4L, List.of(
                new CreateSeatRequest(" a ", 1, SeatType.REGULAR),
                new CreateSeatRequest("A", 1, SeatType.PREMIUM)))));
        verify(seats, never()).saveAll(any());
    }
    @Test void rejectsExistingPositionBeforeSavingLayout() {
        when(screens.findById(4L)).thenReturn(Optional.of(screen()));
        when(seats.existsByScreenIdAndRowLabelIgnoreCaseAndSeatNumber(4L, "A", 1)).thenReturn(true);
        error(DUPLICATE_RESOURCE, () -> service.createSeatLayout(new CreateSeatLayoutRequest(4L,
                List.of(new CreateSeatRequest("a", 1, SeatType.REGULAR)))));
        verify(seats, never()).saveAll(any());
    }
    @Test void rejectsMissingScreen() {
        error(RESOURCE_NOT_FOUND, () -> service.createSeatLayout(new CreateSeatLayoutRequest(99L, List.of())));
        verifyNoInteractions(seats);
    }
    @Test void findsSeatAndMapsScreen() {
        when(seats.findById(110L)).thenReturn(Optional.of(showSeat(10L, ShowSeatStatus.AVAILABLE).getSeat()));
        var result = service.getSeatById(110L);
        assertEquals("A", result.rowLabel()); assertEquals(10, result.seatNumber());
        assertEquals(4L, result.screenId());
    }
    @Test void rejectsMissingSeat() { error(RESOURCE_NOT_FOUND, () -> service.getSeatById(99L)); }
    @Test void listsScreenSeats() {
        when(screens.existsById(4L)).thenReturn(true);
        when(seats.findByScreenId(4L)).thenReturn(List.of(showSeat(10L, ShowSeatStatus.AVAILABLE).getSeat()));
        assertEquals(1, service.getSeatsByScreen(4L).size());
    }
    @Test void returnsEmptyLayoutForExistingScreen() {
        when(screens.existsById(4L)).thenReturn(true);
        assertTrue(service.getSeatsByScreen(4L).isEmpty());
    }
    @Test void rejectsListingMissingScreen() {
        error(RESOURCE_NOT_FOUND, () -> service.getSeatsByScreen(99L)); verifyNoInteractions(seats);
    }
}

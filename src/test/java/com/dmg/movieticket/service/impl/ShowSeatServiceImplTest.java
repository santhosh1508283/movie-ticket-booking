package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.entity.*;
import com.dmg.movieticket.mapper.ShowSeatMapper;
import com.dmg.movieticket.repository.*;
import org.junit.jupiter.api.*;
import org.mapstruct.factory.Mappers;
import java.util.List;
import static com.dmg.movieticket.support.TestFixtures.*;
import static com.dmg.movieticket.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShowSeatServiceImplTest {
    ShowRepository shows = mock(ShowRepository.class);
    ShowSeatRepository seats = mock(ShowSeatRepository.class);
    ShowSeatServiceImpl service = new ShowSeatServiceImpl(shows, seats, Mappers.getMapper(ShowSeatMapper.class));
    @Test void returnsSeatAvailabilityPriceAndLabel() {
        when(shows.existsById(5L)).thenReturn(true);
        when(seats.findByShowId(5L)).thenReturn(List.of(showSeat(10L, ShowSeatStatus.BOOKED)));
        var result = service.getSeatsByShow(5L).get(0);
        assertEquals(10L, result.showSeatId()); assertEquals("A10", result.seatLabel());
        assertEquals(ShowSeatStatus.BOOKED, result.status()); assertEquals(money("150.00"), result.price());
    }
    @Test void returnsEmptyList() {
        when(shows.existsById(5L)).thenReturn(true); assertTrue(service.getSeatsByShow(5L).isEmpty());
    }
    @Test void rejectsMissingShow() {
        error(RESOURCE_NOT_FOUND, () -> service.getSeatsByShow(99L)); verifyNoInteractions(seats);
    }
}

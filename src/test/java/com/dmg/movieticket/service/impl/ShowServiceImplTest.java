package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.CreateShowRequest;
import com.dmg.movieticket.entity.*;
import com.dmg.movieticket.mapper.ShowMapper;
import com.dmg.movieticket.repository.*;
import com.dmg.movieticket.resolver.PricingStrategyResolver;
import com.dmg.movieticket.strategy.pricing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import java.time.LocalDateTime;
import java.util.*;
import static com.dmg.movieticket.support.TestFixtures.*;
import static com.dmg.movieticket.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShowServiceImplTest {
    ShowRepository shows = mock(ShowRepository.class);
    MovieRepository movies = mock(MovieRepository.class);
    ScreenRepository screens = mock(ScreenRepository.class);
    SeatRepository seats = mock(SeatRepository.class);
    ShowSeatRepository showSeats = mock(ShowSeatRepository.class);
    ShowServiceImpl service = new ShowServiceImpl(shows, movies, screens, seats, showSeats,
            Mappers.getMapper(ShowMapper.class),
            new PricingStrategyResolver(new WeekendPricingStrategy(), new DefaultPricingStrategy()));
    LocalDateTime start = LocalDateTime.of(2030, 1, 5, 18, 0);
    CreateShowRequest request() { return new CreateShowRequest(6L, 4L, start, money("100"), money("200")); }
    void parents() {
        when(movies.findById(6L)).thenReturn(Optional.of(show().getMovie()));
        when(screens.findById(4L)).thenReturn(Optional.of(screen()));
    }
    @Test void createsShowWithDurationAndPricesActiveSeats() {
        parents();
        Seat regular = showSeat(10L, ShowSeatStatus.AVAILABLE).getSeat();
        Seat premium = showSeat(11L, ShowSeatStatus.AVAILABLE).getSeat(); premium.setSeatType(SeatType.PREMIUM);
        when(seats.findByScreenIdAndActiveTrue(4L)).thenReturn(List.of(regular, premium));
        when(shows.save(any())).thenAnswer(i -> { Show s = i.getArgument(0); s.setId(5L); return s; });
        var result = service.createShow(request());
        assertEquals(start.plusMinutes(169), result.endTime());
        assertEquals("PVR", result.theaterName()); assertEquals(ShowStatus.SCHEDULED, result.status());
        ArgumentCaptor<List<ShowSeat>> captured = ArgumentCaptor.forClass(List.class);
        verify(showSeats).saveAll(captured.capture());
        var created = captured.getValue();
        assertEquals(2, created.size());
        assertEquals(0, money("120").compareTo(created.get(0).getPrice()));
        assertEquals(0, money("240").compareTo(created.get(1).getPrice()));
        assertTrue(created.stream().allMatch(s -> s.getStatus() == ShowSeatStatus.AVAILABLE && s.getShow().getId() == 5L));
        verify(shows).findOverlappingShows(4L, start, start.plusMinutes(169));
    }
    @Test void rejectsMissingMovie() {
        error(RESOURCE_NOT_FOUND, () -> service.createShow(request())); verifyNoInteractions(showSeats);
    }
    @Test void rejectsMissingScreen() {
        when(movies.findById(6L)).thenReturn(Optional.of(show().getMovie()));
        error(RESOURCE_NOT_FOUND, () -> service.createShow(request())); verify(shows, never()).save(any());
    }
    @Test void rejectsOverlapBeforeSaving() {
        parents(); when(shows.findOverlappingShows(anyLong(), any(), any())).thenReturn(List.of(show()));
        error(INVALID_STATE, () -> service.createShow(request()));
        verify(shows, never()).save(any()); verifyNoInteractions(showSeats);
    }
    @Test void rejectsScreenWithoutActiveSeats() {
        parents(); error(INVALID_STATE, () -> service.createShow(request())); verifyNoInteractions(showSeats);
    }
    @Test void findsShow() {
        when(shows.findById(5L)).thenReturn(Optional.of(show()));
        assertEquals("Interstellar", service.getShowById(5L).movieTitle());
    }
    @Test void rejectsMissingShow() { error(RESOURCE_NOT_FOUND, () -> service.getShowById(99L)); }
    @Test void listsMovieAndScreenShows() {
        when(movies.existsById(6L)).thenReturn(true); when(screens.existsById(4L)).thenReturn(true);
        when(shows.findByMovieId(6L)).thenReturn(List.of(show()));
        when(shows.findByScreenId(4L)).thenReturn(List.of(show()));
        assertEquals(5L, service.getShowsByMovie(6L).get(0).id());
        assertEquals(5L, service.getShowsByScreen(4L).get(0).id());
    }
    @Test void rejectsMissingParentsForLists() {
        error(RESOURCE_NOT_FOUND, () -> service.getShowsByMovie(99L));
        error(RESOURCE_NOT_FOUND, () -> service.getShowsByScreen(99L));
        error(RESOURCE_NOT_FOUND, () -> service.getShowsByMovieAndDateRange(99L, start, start.plusDays(1)));
        verifyNoInteractions(shows);
    }
    @Test void returnsEmptyLists() {
        when(movies.existsById(6L)).thenReturn(true); when(screens.existsById(4L)).thenReturn(true);
        assertTrue(service.getShowsByMovie(6L).isEmpty()); assertTrue(service.getShowsByScreen(4L).isEmpty());
    }
    @ParameterizedTest @ValueSource(ints = {0, -1})
    void rejectsEqualOrReversedDateRange(int days) {
        when(movies.existsById(6L)).thenReturn(true);
        error(INVALID_REQUEST, () -> service.getShowsByMovieAndDateRange(6L, start, start.plusDays(days)));
        verifyNoInteractions(shows);
    }
    @Test void rejectsNullRangeEndpoints() {
        when(movies.existsById(6L)).thenReturn(true);
        error(INVALID_REQUEST, () -> service.getShowsByMovieAndDateRange(6L, null, start));
        error(INVALID_REQUEST, () -> service.getShowsByMovieAndDateRange(6L, start, null));
    }
    @Test void filtersValidDateRange() {
        when(movies.existsById(6L)).thenReturn(true);
        when(shows.findByMovieIdAndStartTimeBetween(6L, start, start.plusDays(1))).thenReturn(List.of(show()));
        assertEquals(1, service.getShowsByMovieAndDateRange(6L, start, start.plusDays(1)).size());
    }
}

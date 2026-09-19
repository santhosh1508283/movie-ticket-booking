package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.CreateScreenRequest;
import com.dmg.movieticket.dto.response.ScreenResponse;
import com.dmg.movieticket.entity.Screen;
import com.dmg.movieticket.entity.Theater;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.mapper.ScreenMapper;
import com.dmg.movieticket.repository.ScreenRepository;
import com.dmg.movieticket.repository.TheaterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ScreenServiceImplTest {

    private ScreenRepository screenRepository;
    private TheaterRepository theaterRepository;
    private ScreenServiceImpl service;
    private final Theater theater = Theater.builder().id(1L).name("PVR").build();

    @BeforeEach
    void setUp() {
        screenRepository = mock(ScreenRepository.class);
        theaterRepository = mock(TheaterRepository.class);
        service = new ScreenServiceImpl(screenRepository, theaterRepository,
                Mappers.getMapper(ScreenMapper.class));
    }

    @Test
    void createsScreenWithTrimmedNameAndTheaterDetails() {
        when(theaterRepository.findById(1L)).thenReturn(Optional.of(theater));
        when(screenRepository.save(any(Screen.class))).thenAnswer(invocation -> {
            Screen screen = invocation.getArgument(0);
            assertNull(screen.getId());
            assertSame(theater, screen.getTheater());
            screen.setId(10L);
            return screen;
        });

        assertEquals(response(10L, "Screen 1"),
                service.createScreen(new CreateScreenRequest(" Screen 1 ", 1L)));
        verify(screenRepository).existsByTheaterIdAndNameIgnoreCase(1L, "Screen 1");
        verify(screenRepository).save(any(Screen.class));
    }

    @Test
    void rejectsCreationForMissingTheaterWithoutSaving() {
        when(theaterRepository.findById(99L)).thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> service.createScreen(new CreateScreenRequest("Screen 1", 99L)));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        assertEquals("Theater not found with id: 99", exception.getMessage());
        verifyNoInteractions(screenRepository);
    }

    @Test
    void rejectsDuplicateNameWithinTheaterWithoutSaving() {
        when(theaterRepository.findById(1L)).thenReturn(Optional.of(theater));
        when(screenRepository.existsByTheaterIdAndNameIgnoreCase(1L, "screen 1")).thenReturn(true);

        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> service.createScreen(new CreateScreenRequest(" screen 1 ", 1L)));

        assertEquals(ErrorCode.DUPLICATE_RESOURCE, exception.getErrorCode());
        verify(screenRepository, never()).save(any());
    }

    @Test
    void allowsSameScreenNameInDifferentTheaters() {
        Theater otherTheater = Theater.builder().id(2L).name("INOX").build();
        when(theaterRepository.findById(1L)).thenReturn(Optional.of(theater));
        when(theaterRepository.findById(2L)).thenReturn(Optional.of(otherTheater));
        when(screenRepository.existsByTheaterIdAndNameIgnoreCase(1L, "Screen 1")).thenReturn(false);
        when(screenRepository.existsByTheaterIdAndNameIgnoreCase(2L, "Screen 1")).thenReturn(false);
        when(screenRepository.save(any(Screen.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(new ScreenResponse(null, "Screen 1", 1L, "PVR"),
                service.createScreen(new CreateScreenRequest("Screen 1", 1L)));
        assertEquals(new ScreenResponse(null, "Screen 1", 2L, "INOX"),
                service.createScreen(new CreateScreenRequest("Screen 1", 2L)));
        verify(screenRepository).existsByTheaterIdAndNameIgnoreCase(1L, "Screen 1");
        verify(screenRepository).existsByTheaterIdAndNameIgnoreCase(2L, "Screen 1");
        verify(screenRepository, times(2)).save(any(Screen.class));
    }

    @Test
    void findsScreenWithTheaterDetails() {
        when(screenRepository.findById(10L)).thenReturn(Optional.of(screen(10L, "Screen 1")));

        assertEquals(response(10L, "Screen 1"), service.getScreenById(10L));
    }

    @Test
    void rejectsMissingScreen() {
        when(screenRepository.findById(99L)).thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> service.getScreenById(99L));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        assertEquals("Screen not found with id: 99", exception.getMessage());
    }

    @Test
    void listsScreensInRepositoryOrder() {
        when(screenRepository.findAll()).thenReturn(List.of(screen(10L, "Screen 1"), screen(11L, "Screen 2")));

        assertEquals(List.of(response(10L, "Screen 1"), response(11L, "Screen 2")), service.getAllScreens());
    }

    @Test
    void returnsEmptyListWhenNoScreensExist() {
        when(screenRepository.findAll()).thenReturn(List.of());

        assertTrue(service.getAllScreens().isEmpty());
        verify(screenRepository).findAll();
    }

    @Test
    void filtersScreensByExistingTheater() {
        when(theaterRepository.existsById(1L)).thenReturn(true);
        when(screenRepository.findByTheaterId(1L)).thenReturn(List.of(screen(10L, "Screen 1")));

        assertEquals(List.of(response(10L, "Screen 1")), service.getScreensByTheater(1L));
        verify(screenRepository).findByTheaterId(1L);
    }

    @Test
    void returnsEmptyListForTheaterWithoutScreens() {
        when(theaterRepository.existsById(1L)).thenReturn(true);
        when(screenRepository.findByTheaterId(1L)).thenReturn(List.of());

        assertTrue(service.getScreensByTheater(1L).isEmpty());
        verify(screenRepository).findByTheaterId(1L);
    }

    @Test
    void rejectsFilteringByMissingTheater() {
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> service.getScreensByTheater(99L));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(screenRepository);
    }

    private Screen screen(Long id, String name) {
        return Screen.builder().id(id).name(name).theater(theater).build();
    }

    private ScreenResponse response(Long id, String name) {
        return new ScreenResponse(id, name, 1L, "PVR");
    }
}

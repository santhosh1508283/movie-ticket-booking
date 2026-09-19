package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.CreateTheaterRequest;
import com.dmg.movieticket.dto.response.TheaterResponse;
import com.dmg.movieticket.entity.City;
import com.dmg.movieticket.entity.Theater;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.mapper.TheaterMapper;
import com.dmg.movieticket.repository.CityRepository;
import com.dmg.movieticket.repository.TheaterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TheaterServiceImplTest {

    private TheaterRepository theaterRepository;
    private CityRepository cityRepository;
    private TheaterServiceImpl service;
    private final City city = City.builder().id(1L).name("Bengaluru").build();

    @BeforeEach
    void setUp() {
        theaterRepository = mock(TheaterRepository.class);
        cityRepository = mock(CityRepository.class);
        service = new TheaterServiceImpl(theaterRepository, cityRepository,
                Mappers.getMapper(TheaterMapper.class));
    }

    @Test
    void createsTheaterWithNormalizedFieldsAndCityDetails() {
        when(cityRepository.findById(1L)).thenReturn(Optional.of(city));
        when(theaterRepository.save(any(Theater.class))).thenAnswer(invocation -> {
            Theater theater = invocation.getArgument(0);
            assertSame(city, theater.getCity());
            theater.setId(10L);
            return theater;
        });

        TheaterResponse response = service.createTheater(new CreateTheaterRequest(
                " PVR ", " Main Road ", " Mall ", " 560001 ", 1L));

        assertEquals(new TheaterResponse(10L, "PVR", "Main Road", "Mall", "560001",
                1L, "Bengaluru"), response);
        verify(theaterRepository).existsByNameIgnoreCaseAndCityId("PVR", 1L);
    }

    @Test
    void allowsMissingOptionalFields() {
        when(cityRepository.findById(1L)).thenReturn(Optional.of(city));
        when(theaterRepository.save(any(Theater.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TheaterResponse response = service.createTheater(request());

        assertNull(response.landmark());
        assertNull(response.postalCode());
    }

    @Test
    void rejectsCreationForMissingCityWithoutSaving() {
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> service.createTheater(request()));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(theaterRepository);
    }

    @Test
    void rejectsDuplicateNameWithinCityWithoutSaving() {
        when(cityRepository.findById(1L)).thenReturn(Optional.of(city));
        when(theaterRepository.existsByNameIgnoreCaseAndCityId("PVR", 1L)).thenReturn(true);

        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> service.createTheater(request()));

        assertEquals(ErrorCode.DUPLICATE_RESOURCE, exception.getErrorCode());
        verify(theaterRepository, never()).save(any());
    }

    @Test
    void findsTheaterWithMappedCityDetails() {
        when(theaterRepository.findById(10L)).thenReturn(Optional.of(theater()));

        assertEquals(expectedResponse(), service.getTheaterById(10L));
    }

    @Test
    void rejectsMissingTheater() {
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> service.getTheaterById(99L));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void listsTheaters() {
        when(theaterRepository.findAll()).thenReturn(List.of(theater()));

        assertEquals(List.of(expectedResponse()), service.getAllTheaters());
    }

    @Test
    void filtersByExistingCity() {
        when(cityRepository.existsById(1L)).thenReturn(true);
        when(theaterRepository.findByCityId(1L)).thenReturn(List.of(theater()));

        assertEquals(List.of(expectedResponse()), service.getTheatersByCity(1L));
    }

    @Test
    void returnsEmptyListForCityWithoutTheaters() {
        when(cityRepository.existsById(1L)).thenReturn(true);

        assertTrue(service.getTheatersByCity(1L).isEmpty());
    }

    @Test
    void rejectsFilteringByMissingCity() {
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> service.getTheatersByCity(99L));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(theaterRepository);
    }

    @Test
    void trimsSearchTermAndMapsResults() {
        when(theaterRepository.findByNameContainingIgnoreCase("pvr")).thenReturn(List.of(theater()));

        assertEquals(List.of(expectedResponse()), service.searchTheaters(" pvr "));
    }

    private CreateTheaterRequest request() {
        return new CreateTheaterRequest(" PVR ", " Main Road ", null, null, 1L);
    }

    private Theater theater() {
        return Theater.builder().id(10L).name("PVR").address("Main Road").city(city).build();
    }

    private TheaterResponse expectedResponse() {
        return new TheaterResponse(10L, "PVR", "Main Road", null, null, 1L, "Bengaluru");
    }
}

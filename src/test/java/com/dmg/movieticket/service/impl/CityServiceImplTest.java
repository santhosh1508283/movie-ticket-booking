package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.CreateCityRequest;
import com.dmg.movieticket.dto.response.CityResponse;
import com.dmg.movieticket.entity.City;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.mapper.CityMapper;
import com.dmg.movieticket.repository.CityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CityServiceImplTest {

    private CityRepository cityRepository;
    private CityServiceImpl service;

    @BeforeEach
    void setUp() {
        cityRepository = mock(CityRepository.class);
        service = new CityServiceImpl(cityRepository, Mappers.getMapper(CityMapper.class));
    }

    @Test
    void createsCityWithTrimmedNameAndGeneratedId() {
        when(cityRepository.save(any(City.class))).thenAnswer(invocation -> {
            City city = invocation.getArgument(0);
            assertNull(city.getId());
            city.setId(1L);
            return city;
        });

        assertEquals(new CityResponse(1L, "New Delhi"),
                service.createCity(new CreateCityRequest("  New Delhi  ")));
        verify(cityRepository).existsByNameIgnoreCase("New Delhi");
        verify(cityRepository).save(any(City.class));
    }

    @Test
    void rejectsDuplicateCityWithoutSaving() {
        when(cityRepository.existsByNameIgnoreCase("bengaluru")).thenReturn(true);

        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> service.createCity(new CreateCityRequest(" bengaluru ")));

        assertEquals(ErrorCode.DUPLICATE_RESOURCE, exception.getErrorCode());
        assertEquals("City already exists with name: bengaluru", exception.getMessage());
        verify(cityRepository, never()).save(any());
    }

    @Test
    void findsCityById() {
        when(cityRepository.findById(1L)).thenReturn(Optional.of(city(1L, "Bengaluru")));

        assertEquals(new CityResponse(1L, "Bengaluru"), service.getCityById(1L));
    }

    @Test
    void rejectsMissingCity() {
        when(cityRepository.findById(99L)).thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> service.getCityById(99L));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        assertEquals("City not found with id: 99", exception.getMessage());
    }

    @Test
    void listsAllCitiesInRepositoryOrder() {
        when(cityRepository.findAll()).thenReturn(List.of(city(1L, "Bengaluru"), city(2L, "Mumbai")));

        assertEquals(List.of(new CityResponse(1L, "Bengaluru"), new CityResponse(2L, "Mumbai")),
                service.getAllCities());
    }

    @Test
    void returnsEmptyListWhenNoCitiesExist() {
        when(cityRepository.findAll()).thenReturn(List.of());

        assertTrue(service.getAllCities().isEmpty());
        verify(cityRepository).findAll();
    }

    private City city(Long id, String name) {
        return City.builder().id(id).name(name).build();
    }
}

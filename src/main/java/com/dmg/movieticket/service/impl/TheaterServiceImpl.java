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
import com.dmg.movieticket.service.TheaterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TheaterServiceImpl implements TheaterService {

    private final TheaterRepository theaterRepository;
    private final CityRepository cityRepository;
    private final TheaterMapper theaterMapper;

    @Override
    @Transactional
    public TheaterResponse createTheater(CreateTheaterRequest request) {

        City city = cityRepository.findById(request.cityId())
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "City not found with id: " + request.cityId()
                ));

        String normalizedName = request.name().trim();

        if (theaterRepository.existsByNameIgnoreCaseAndCityId(normalizedName, request.cityId())) {
            throw new ApplicationException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "Theater already exists with name: " + normalizedName + " in city: " + request.cityId()
            );
        }

        Theater theater = Theater.builder()
                .name(normalizedName)
                .address(request.address().trim())
                .landmark(request.landmark() != null ? request.landmark().trim() : null)
                .postalCode(request.postalCode() != null ? request.postalCode().trim() : null)
                .city(city)
                .build();

        return theaterMapper.toResponse(theaterRepository.save(theater));
    }

    @Override
    @Transactional(readOnly = true)
    public TheaterResponse getTheaterById(Long theaterId) {

        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Theater not found with id: " + theaterId
                ));

        return theaterMapper.toResponse(theater);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TheaterResponse> getAllTheaters() {

        return theaterRepository.findAll()
                .stream()
                .map(theaterMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TheaterResponse> getTheatersByCity(Long cityId) {

        if (!cityRepository.existsById(cityId)) {
            throw new ApplicationException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "City not found with id: " + cityId
            );
        }

        return theaterRepository.findByCityId(cityId)
                .stream()
                .map(theaterMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TheaterResponse> searchTheaters(String name) {

        return theaterRepository.findByNameContainingIgnoreCase(name.trim())
                .stream()
                .map(theaterMapper::toResponse)
                .toList();
    }
}

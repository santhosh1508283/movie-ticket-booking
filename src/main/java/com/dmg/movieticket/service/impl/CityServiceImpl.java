package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.CreateCityRequest;
import com.dmg.movieticket.dto.response.CityResponse;
import com.dmg.movieticket.entity.City;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.mapper.CityMapper;
import com.dmg.movieticket.repository.CityRepository;
import com.dmg.movieticket.service.CityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityServiceImpl implements CityService {

    private final CityRepository cityRepository;
    private final CityMapper cityMapper;

    @Override
    @Transactional
    public CityResponse createCity(CreateCityRequest request) {

        String normalizedName = request.name().trim();

        if (cityRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ApplicationException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "City already exists with name: " + normalizedName
            );
        }

        City city = City.builder()
                .name(normalizedName)
                .build();

        return cityMapper.toResponse(cityRepository.save(city));
    }

    @Override
    @Transactional(readOnly = true)
    public CityResponse getCityById(Long cityId) {

        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "City not found with id: " + cityId
                ));

        return cityMapper.toResponse(city);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CityResponse> getAllCities() {

        return cityRepository.findAll()
                .stream()
                .map(cityMapper::toResponse)
                .toList();
    }
}
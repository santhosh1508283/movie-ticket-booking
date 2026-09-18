package com.dmg.movieticket.service;

import com.dmg.movieticket.dto.request.CreateCityRequest;
import com.dmg.movieticket.dto.response.CityResponse;

import java.util.List;

public interface CityService {

    CityResponse createCity(CreateCityRequest request);

    CityResponse getCityById(Long cityId);

    List<CityResponse> getAllCities();
}
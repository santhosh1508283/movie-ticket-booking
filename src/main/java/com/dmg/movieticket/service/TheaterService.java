package com.dmg.movieticket.service;

import com.dmg.movieticket.dto.request.CreateTheaterRequest;
import com.dmg.movieticket.dto.response.TheaterResponse;

import java.util.List;

public interface TheaterService {

    TheaterResponse createTheater(CreateTheaterRequest request);

    TheaterResponse getTheaterById(Long theaterId);

    List<TheaterResponse> getAllTheaters();

    List<TheaterResponse> getTheatersByCity(Long cityId);

    List<TheaterResponse> searchTheaters(String name);
}

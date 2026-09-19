package com.dmg.movieticket.service;

import com.dmg.movieticket.dto.request.CreateScreenRequest;
import com.dmg.movieticket.dto.response.ScreenResponse;

import java.util.List;

public interface ScreenService {

    ScreenResponse createScreen(CreateScreenRequest request);

    ScreenResponse getScreenById(Long screenId);

    List<ScreenResponse> getAllScreens();

    List<ScreenResponse> getScreensByTheater(Long theaterId);
}

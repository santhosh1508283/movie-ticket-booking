package com.dmg.movieticket.service;

import com.dmg.movieticket.dto.request.CreateSeatHoldRequest;
import com.dmg.movieticket.dto.response.SeatHoldResponse;

public interface SeatHoldService {

    SeatHoldResponse createHold(CreateSeatHoldRequest request);

    SeatHoldResponse getHold(Long holdId);

    void releaseHold(Long holdId);
}
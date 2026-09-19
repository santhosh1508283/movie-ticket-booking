package com.dmg.movieticket.service;

import com.dmg.movieticket.dto.request.CreateSeatLayoutRequest;
import com.dmg.movieticket.dto.response.SeatResponse;

import java.util.List;

public interface SeatService {

    List<SeatResponse> createSeatLayout(CreateSeatLayoutRequest request);

    SeatResponse getSeatById(Long seatId);

    List<SeatResponse> getSeatsByScreen(Long screenId);
}
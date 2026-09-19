package com.dmg.movieticket.service;

import com.dmg.movieticket.dto.response.ShowSeatResponse;

import java.util.List;

public interface ShowSeatService {

    List<ShowSeatResponse> getSeatsByShow(Long showId);
}
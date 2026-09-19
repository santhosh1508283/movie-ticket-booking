package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.CreateSeatLayoutRequest;
import com.dmg.movieticket.dto.request.CreateSeatRequest;
import com.dmg.movieticket.dto.response.SeatResponse;
import com.dmg.movieticket.entity.Screen;
import com.dmg.movieticket.entity.Seat;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.mapper.SeatMapper;
import com.dmg.movieticket.repository.ScreenRepository;
import com.dmg.movieticket.repository.SeatRepository;
import com.dmg.movieticket.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final ScreenRepository screenRepository;
    private final SeatMapper seatMapper;

    @Override
    @Transactional
    public List<SeatResponse> createSeatLayout(CreateSeatLayoutRequest request) {

        Screen screen = screenRepository.findById(request.screenId())
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Screen not found with id: " + request.screenId()
                ));

        Set<String> requestSeatKeys = new HashSet<>();

        for (CreateSeatRequest seatRequest : request.seats()) {

            String rowLabel = seatRequest.rowLabel().trim().toUpperCase();
            String seatKey = rowLabel + "-" + seatRequest.seatNumber();

            if (!requestSeatKeys.add(seatKey)) {
                throw new ApplicationException(
                        ErrorCode.DUPLICATE_RESOURCE,
                        "Duplicate seat in request: "
                                + rowLabel
                                + seatRequest.seatNumber()
                );
            }

            if (seatRepository.existsByScreenIdAndRowLabelIgnoreCaseAndSeatNumber(
                    request.screenId(),
                    rowLabel,
                    seatRequest.seatNumber()
            )) {
                throw new ApplicationException(
                        ErrorCode.DUPLICATE_RESOURCE,
                        "Seat already exists: "
                                + rowLabel
                                + seatRequest.seatNumber()
                );
            }
        }

        List<Seat> seats = request.seats()
                .stream()
                .map(seatRequest -> Seat.builder()
                        .rowLabel(seatRequest.rowLabel().trim().toUpperCase())
                        .seatNumber(seatRequest.seatNumber())
                        .seatType(seatRequest.seatType())
                        .screen(screen)
                        .build()
                )
                .toList();

        return seatRepository.saveAll(seats)
                .stream()
                .map(seatMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SeatResponse getSeatById(Long seatId) {

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Seat not found with id: " + seatId
                ));

        return seatMapper.toResponse(seat);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByScreen(Long screenId) {

        if (!screenRepository.existsById(screenId)) {
            throw new ApplicationException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Screen not found with id: " + screenId
            );
        }

        return seatRepository.findByScreenId(screenId)
                .stream()
                .map(seatMapper::toResponse)
                .toList();
    }
}
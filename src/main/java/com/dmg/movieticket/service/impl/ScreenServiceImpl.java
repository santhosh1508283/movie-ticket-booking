package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.CreateScreenRequest;
import com.dmg.movieticket.dto.response.ScreenResponse;
import com.dmg.movieticket.entity.Screen;
import com.dmg.movieticket.entity.Theater;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.mapper.ScreenMapper;
import com.dmg.movieticket.repository.ScreenRepository;
import com.dmg.movieticket.repository.TheaterRepository;
import com.dmg.movieticket.service.ScreenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScreenServiceImpl implements ScreenService {

    private final ScreenRepository screenRepository;
    private final TheaterRepository theaterRepository;
    private final ScreenMapper screenMapper;

    @Override
    @Transactional
    public ScreenResponse createScreen(CreateScreenRequest request) {

        Theater theater = theaterRepository.findById(request.theaterId())
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Theater not found with id: " + request.theaterId()
                ));

        String normalizedName = request.name().trim();

        if (screenRepository.existsByTheaterIdAndNameIgnoreCase(request.theaterId(), normalizedName)) {
            throw new ApplicationException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "Screen already exists with name: " + normalizedName + " in theater: " + request.theaterId()
            );
        }

        Screen screen = Screen.builder()
                .name(normalizedName)
                .theater(theater)
                .build();

        return screenMapper.toResponse(screenRepository.save(screen));
    }

    @Override
    @Transactional(readOnly = true)
    public ScreenResponse getScreenById(Long screenId) {

        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Screen not found with id: " + screenId
                ));

        return screenMapper.toResponse(screen);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScreenResponse> getAllScreens() {

        return screenRepository.findAll()
                .stream()
                .map(screenMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScreenResponse> getScreensByTheater(Long theaterId) {

        if (!theaterRepository.existsById(theaterId)) {
            throw new ApplicationException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Theater not found with id: " + theaterId
            );
        }

        return screenRepository.findByTheaterId(theaterId)
                .stream()
                .map(screenMapper::toResponse)
                .toList();
    }
}

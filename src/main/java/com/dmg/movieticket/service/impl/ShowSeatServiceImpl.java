package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.response.ShowSeatResponse;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.mapper.ShowSeatMapper;
import com.dmg.movieticket.repository.ShowRepository;
import com.dmg.movieticket.repository.ShowSeatRepository;
import com.dmg.movieticket.service.ShowSeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShowSeatServiceImpl implements ShowSeatService {

    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ShowSeatMapper showSeatMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ShowSeatResponse> getSeatsByShow(Long showId) {

        if (!showRepository.existsById(showId)) {
            throw new ApplicationException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Show not found with id: " + showId
            );
        }

        return showSeatRepository.findByShowId(showId)
                .stream()
                .map(showSeatMapper::toResponse)
                .toList();
    }
}
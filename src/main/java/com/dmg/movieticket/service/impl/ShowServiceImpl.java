package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.CreateShowRequest;
import com.dmg.movieticket.dto.response.ShowResponse;
import com.dmg.movieticket.entity.*;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.mapper.ShowMapper;
import com.dmg.movieticket.repository.*;
import com.dmg.movieticket.resolver.PricingStrategyResolver;
import com.dmg.movieticket.service.ShowService;
import com.dmg.movieticket.strategy.pricing.PricingContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShowServiceImpl implements ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ShowMapper showMapper;
    private final PricingStrategyResolver pricingStrategyResolver;

    @Override
    @Transactional
    public ShowResponse createShow(CreateShowRequest request) {

        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Movie not found with id: " + request.movieId()
                ));

        Screen screen = screenRepository.findById(request.screenId())
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Screen not found with id: " + request.screenId()
                ));

        LocalDateTime endTime =
                request.startTime().plusMinutes(movie.getDurationMinutes());

        List<Show> overlappingShows =
                showRepository.findOverlappingShows(
                        request.screenId(),
                        request.startTime(),
                        endTime
                );

        if (!overlappingShows.isEmpty()) {
            throw new ApplicationException(
                    ErrorCode.INVALID_STATE,
                    "Screen already has an overlapping show"
            );
        }

        Show show = Show.builder()
                .movie(movie)
                .screen(screen)
                .startTime(request.startTime())
                .endTime(endTime)
                .status(ShowStatus.SCHEDULED)
                .build();

        Show savedShow = showRepository.save(show);

        List<Seat> seats = seatRepository.findByScreenIdAndActiveTrue(request.screenId());
        if (seats.isEmpty()) {
            throw new ApplicationException(
                    ErrorCode.INVALID_STATE,
                    "Cannot create show because the screen has no seats"
            );
        }

        List<ShowSeat> showSeats = seats.stream()
                .map(seat -> {
                    BigDecimal basePrice =
                            seat.getSeatType() == SeatType.PREMIUM
                                    ? request.premiumBasePrice()
                                    : request.regularBasePrice();

                    PricingContext context = new PricingContext(
                            seat.getSeatType(),
                            basePrice,
                            request.startTime()
                    );

                    BigDecimal finalPrice =
                            pricingStrategyResolver
                                    .resolve(context)
                                    .calculate(context);

                    return ShowSeat.builder()
                            .show(savedShow)
                            .seat(seat)
                            .price(finalPrice)
                            .status(ShowSeatStatus.AVAILABLE)
                            .build();
                })
                .toList();

        showSeatRepository.saveAll(showSeats);

        return showMapper.toResponse(savedShow);
    }

    @Override
    @Transactional(readOnly = true)
    public ShowResponse getShowById(Long showId) {

        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Show not found with id: " + showId
                ));

        return showMapper.toResponse(show);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsByMovie(Long movieId) {

        if (!movieRepository.existsById(movieId)) {
            throw new ApplicationException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Movie not found with id: " + movieId
            );
        }

        return showRepository.findByMovieId(movieId)
                .stream()
                .map(showMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsByScreen(Long screenId) {

        if (!screenRepository.existsById(screenId)) {
            throw new ApplicationException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Screen not found with id: " + screenId
            );
        }

        return showRepository.findByScreenId(screenId)
                .stream()
                .map(showMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsByMovieAndDateRange(
            Long movieId,
            LocalDateTime start,
            LocalDateTime end
    ) {

        if (!movieRepository.existsById(movieId)) {
            throw new ApplicationException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Movie not found with id: " + movieId
            );
        }

        if (start == null || end == null || !start.isBefore(end)) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "Invalid date range"
            );
        }

        return showRepository
                .findByMovieIdAndStartTimeBetween(movieId, start, end)
                .stream()
                .map(showMapper::toResponse)
                .toList();
    }
}
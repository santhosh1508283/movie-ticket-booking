package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.CreateSeatHoldRequest;
import com.dmg.movieticket.dto.response.SeatHoldResponse;
import com.dmg.movieticket.entity.*;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.mapper.SeatHoldMapper;
import com.dmg.movieticket.repository.SeatHoldItemRepository;
import com.dmg.movieticket.repository.SeatHoldRepository;
import com.dmg.movieticket.repository.ShowRepository;
import com.dmg.movieticket.repository.ShowSeatRepository;
import com.dmg.movieticket.security.CurrentUserService;
import com.dmg.movieticket.service.SeatHoldService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatHoldServiceImpl implements SeatHoldService {

    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final SeatHoldItemRepository seatHoldItemRepository;
    private final SeatHoldMapper seatHoldMapper;
    private final CurrentUserService currentUserService;

    @Value("${booking.seat-hold-duration-minutes:5}")
    private long holdDurationMinutes;

    @Override
    @Transactional
    public SeatHoldResponse createHold(CreateSeatHoldRequest request) {

        User user = currentUserService.getCurrentUser();

        Show show = showRepository.findById(request.showId())
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Show not found with id: " + request.showId()
                ));

        LocalDateTime now = LocalDateTime.now();

        validateShowForHold(show, now);

        List<Long> requestedSeatIds = request.showSeatIds()
                .stream()
                .distinct()
                .sorted()
                .toList();

        if (requestedSeatIds.size() != request.showSeatIds().size()) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "Duplicate show seat ids are not allowed"
            );
        }

        /*
         * PESSIMISTIC_WRITE is applied inside the repository method.
         *
         * These rows stay locked until this transaction completes.
         */
        List<ShowSeat> showSeats =
                showSeatRepository.findByShowIdAndIdsForUpdate(
                        request.showId(),
                        requestedSeatIds
                );

        if (showSeats.size() != requestedSeatIds.size()) {
            throw new ApplicationException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "One or more selected seats do not exist for this show"
            );
        }

        /*
         * Check every selected seat while we own the DB lock.
         */
        for (ShowSeat showSeat : showSeats) {

            if (showSeat.getStatus() == ShowSeatStatus.BOOKED) {
                throw new ApplicationException(
                        ErrorCode.SEAT_NOT_AVAILABLE,
                        "Seat is already booked: " + getSeatLabel(showSeat)
                );
            }

            if (showSeat.getStatus() == ShowSeatStatus.HELD) {

                handleExistingHold(showSeat, now);
            }
        }

        /*
         * handleExistingHold() may have expired an old hold.
         * At this point every seat must therefore be AVAILABLE.
         */
        for (ShowSeat showSeat : showSeats) {

            if (showSeat.getStatus() != ShowSeatStatus.AVAILABLE) {
                throw new ApplicationException(
                        ErrorCode.SEAT_NOT_AVAILABLE,
                        "Seat is not available: " + getSeatLabel(showSeat)
                );
            }
        }

        SeatHold hold = SeatHold.builder()
                .user(user)
                .show(show)
                .status(HoldStatus.ACTIVE)
                .expiresAt(now.plusMinutes(holdDurationMinutes))
                .build();

        for (ShowSeat showSeat : showSeats) {

            showSeat.setStatus(ShowSeatStatus.HELD);

            SeatHoldItem item = SeatHoldItem.builder()
                    .seatHold(hold)
                    .showSeat(showSeat)
                    .price(showSeat.getPrice())
                    .build();

            /*
             * Keep both sides of the JPA relationship synchronized.
             */
            hold.getItems().add(item);
        }

        SeatHold savedHold = seatHoldRepository.save(hold);

        showSeatRepository.saveAll(showSeats);

        return seatHoldMapper.toResponse(savedHold);
    }

    @Override
    @Transactional
    public SeatHoldResponse getHold(Long holdId) {

        User user = currentUserService.getCurrentUser();

        SeatHold hold = seatHoldRepository.findByIdAndUserId(
                        holdId,
                        user.getId()
                )
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Seat hold not found with id: " + holdId
                ));

        /*
         * We should not return an ACTIVE hold if its expiry time
         * has already passed.
         */
        if (hold.getStatus() == HoldStatus.ACTIVE
                && !hold.getExpiresAt().isAfter(LocalDateTime.now())) {

            expireHold(hold);
        }

        return seatHoldMapper.toResponse(hold);
    }

    @Override
    @Transactional
    public void releaseHold(Long holdId) {

        User user = currentUserService.getCurrentUser();

        SeatHold hold = seatHoldRepository.findByIdAndUserId(
                        holdId,
                        user.getId()
                )
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Seat hold not found with id: " + holdId
                ));

        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new ApplicationException(
                    ErrorCode.INVALID_STATE,
                    "Only active holds can be released"
            );
        }

        if (!hold.getExpiresAt().isAfter(LocalDateTime.now())) {

            expireHold(hold);

            throw new ApplicationException(
                    ErrorCode.HOLD_EXPIRED,
                    "Seat hold has already expired"
            );
        }

        releaseSeats(hold);

        hold.setStatus(HoldStatus.CANCELLED);

        seatHoldRepository.save(hold);
    }

    private void validateShowForHold(
            Show show,
            LocalDateTime now
    ) {

        if (show.getStatus() != ShowStatus.SCHEDULED) {
            throw new ApplicationException(
                    ErrorCode.INVALID_STATE,
                    "Seats cannot be held for a non-scheduled show"
            );
        }

        if (!show.getStartTime().isAfter(now)) {
            throw new ApplicationException(
                    ErrorCode.INVALID_STATE,
                    "Seats cannot be held after the show has started"
            );
        }
    }

    private void handleExistingHold(
            ShowSeat showSeat,
            LocalDateTime now
    ) {

        SeatHoldItem existingItem =
                seatHoldItemRepository
                        .findFirstByShowSeatIdAndSeatHoldStatusOrderBySeatHoldExpiresAtDesc(
                                showSeat.getId(),
                                HoldStatus.ACTIVE
                        )
                        .orElseThrow(() -> new ApplicationException(
                                ErrorCode.INVALID_STATE,
                                "Seat is marked HELD without an active hold"
                        ));

        SeatHold existingHold = existingItem.getSeatHold();

        /*
         * Hold is still valid.
         */
        if (existingHold.getExpiresAt().isAfter(now)) {

            throw new ApplicationException(
                    ErrorCode.SEAT_NOT_AVAILABLE,
                    "Seat is currently held: " + getSeatLabel(showSeat)
            );
        }

        /*
         * Hold expired but scheduler has not cleaned it yet.
         *
         * Expire synchronously.
         */
        expireHold(existingHold);
    }

    private void expireHold(SeatHold hold) {

        if (hold.getStatus() != HoldStatus.ACTIVE) {
            return;
        }

        releaseSeats(hold);

        hold.setStatus(HoldStatus.EXPIRED);

        seatHoldRepository.save(hold);
    }

    private void releaseSeats(SeatHold hold) {

        List<SeatHoldItem> items =
                seatHoldItemRepository.findBySeatHoldId(hold.getId());

        List<Long> showSeatIds = items.stream()
                .map(item -> item.getShowSeat().getId())
                .distinct()
                .sorted()
                .toList();

        if (showSeatIds.isEmpty()) {
            return;
        }

        /*
         * Lock the rows before changing HELD -> AVAILABLE.
         */
        List<ShowSeat> showSeats =
                showSeatRepository.findByIdsForUpdate(showSeatIds);

        for (ShowSeat showSeat : showSeats) {

            if (showSeat.getStatus() == ShowSeatStatus.HELD) {
                showSeat.setStatus(ShowSeatStatus.AVAILABLE);
            }
        }

        showSeatRepository.saveAll(showSeats);
    }

    private String getSeatLabel(ShowSeat showSeat) {

        return showSeat.getSeat().getRowLabel()
                + showSeat.getSeat().getSeatNumber();
    }
}
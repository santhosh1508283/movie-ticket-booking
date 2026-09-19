package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.CreateSeatHoldRequest;
import com.dmg.movieticket.dto.response.SeatHoldResponse;
import com.dmg.movieticket.entity.*;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.mapper.SeatHoldMapper;
import com.dmg.movieticket.repository.*;
import com.dmg.movieticket.service.SeatHoldService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatHoldServiceImpl implements SeatHoldService {

    private final UserRepository userRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final SeatHoldItemRepository seatHoldItemRepository;
    private final SeatHoldMapper seatHoldMapper;

    @Value("${booking.seat-hold-duration-minutes:5}")
    private long holdDurationMinutes;

    @Override
    @Transactional
    public SeatHoldResponse createHold(
            Long userId,
            CreateSeatHoldRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "User not found with id: " + userId
                ));

        Show show = showRepository.findById(request.showId())
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Show not found with id: " + request.showId()
                ));

        LocalDateTime now = LocalDateTime.now();

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

        List<Long> requestedIds = request.showSeatIds()
                .stream()
                .distinct()
                .sorted()
                .toList();

        if (requestedIds.size() != request.showSeatIds().size()) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "Duplicate show seat ids are not allowed"
            );
        }

        List<ShowSeat> showSeats =
                showSeatRepository.findByShowIdAndIdsForUpdate(
                        request.showId(),
                        requestedIds
                );

        if (showSeats.size() != requestedIds.size()) {
            throw new ApplicationException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "One or more selected seats do not exist for this show"
            );
        }

        /*
         * A HELD seat may belong to an already-expired hold.
         * We cannot rely only on the scheduler, so expired holds are
         * cleaned synchronously when encountered.
         */
        for (ShowSeat showSeat : showSeats) {

            if (showSeat.getStatus() == ShowSeatStatus.BOOKED) {
                throw new ApplicationException(
                        ErrorCode.SEAT_NOT_AVAILABLE,
                        "Seat is already booked: " + getSeatLabel(showSeat)
                );
            }

            if (showSeat.getStatus() == ShowSeatStatus.HELD) {

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

                if (existingHold.getExpiresAt().isAfter(now)) {
                    throw new ApplicationException(
                            ErrorCode.SEAT_NOT_AVAILABLE,
                            "Seat is currently held: " + getSeatLabel(showSeat)
                    );
                }

                expireHold(existingHold);
            }
        }

        /*
         * expireHold() may have changed seats to AVAILABLE.
         * Validate final state before assigning the new hold.
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

            hold.getItems().add(item);
        }

        SeatHold savedHold = seatHoldRepository.save(hold);

        showSeatRepository.saveAll(showSeats);

        return seatHoldMapper.toResponse(savedHold);
    }

    @Override
    @Transactional(readOnly = true)
    public SeatHoldResponse getHold(
            Long userId,
            Long holdId
    ) {

        SeatHold hold = seatHoldRepository.findByIdAndUserId(
                        holdId,
                        userId
                )
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Seat hold not found with id: " + holdId
                ));

        return seatHoldMapper.toResponse(hold);
    }

    @Override
    @Transactional
    public void releaseHold(
            Long userId,
            Long holdId
    ) {

        SeatHold hold = seatHoldRepository.findByIdAndUserId(
                        holdId,
                        userId
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

        releaseSeats(hold);

        hold.setStatus(HoldStatus.CANCELLED);
        seatHoldRepository.save(hold);
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

        List<ShowSeat> seats =
                showSeatRepository.findByIdsForUpdate(showSeatIds);

        for (ShowSeat seat : seats) {
            if (seat.getStatus() == ShowSeatStatus.HELD) {
                seat.setStatus(ShowSeatStatus.AVAILABLE);
            }
        }

        showSeatRepository.saveAll(seats);
    }

    private String getSeatLabel(ShowSeat showSeat) {
        return showSeat.getSeat().getRowLabel()
                + showSeat.getSeat().getSeatNumber();
    }
}
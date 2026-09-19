package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.CreateBookingRequest;
import com.dmg.movieticket.dto.response.BookingResponse;
import com.dmg.movieticket.dto.response.DiscountResult;
import com.dmg.movieticket.entity.*;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.mapper.BookingMapper;
import com.dmg.movieticket.repository.BookingRepository;
import com.dmg.movieticket.repository.SeatHoldRepository;
import com.dmg.movieticket.security.CurrentUserService;
import com.dmg.movieticket.service.BookingService;
import com.dmg.movieticket.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final DiscountService discountService;
    private final BookingMapper bookingMapper;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public BookingResponse createBooking(
            CreateBookingRequest request
    ) {

        User user = currentUserService.getCurrentUser();

        /*
         * Lock the hold so concurrent booking creation requests
         * cannot convert the same hold twice.
         */
        SeatHold hold =
                seatHoldRepository.findByIdAndUserIdForUpdate(
                                request.holdId(),
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new ApplicationException(
                                        ErrorCode.RESOURCE_NOT_FOUND,
                                        "Seat hold not found with id: "
                                                + request.holdId()
                                )
                        );

        validateHoldForBooking(hold);

        if (bookingRepository.existsBySeatHoldId(hold.getId())) {
            throw new ApplicationException(
                    ErrorCode.INVALID_STATE,
                    "A booking has already been created for this hold"
            );
        }

        BigDecimal subtotal = hold.getItems()
                .stream()
                .map(SeatHoldItem::getPrice)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );

        DiscountResult discountResult =
                discountService.calculateDiscount(
                        request.discountCode(),
                        subtotal
                );

        BigDecimal totalAmount =
                subtotal.subtract(
                        discountResult.discountAmount()
                );

        Booking booking = Booking.builder()
                .bookingReference(generateBookingReference())
                .user(user)
                .show(hold.getShow())
                .seatHold(hold)
                .subtotal(subtotal)
                .discountAmount(
                        discountResult.discountAmount()
                )
                .appliedDiscountCode(
                        discountResult.code()
                )
                .totalAmount(totalAmount)
                .status(BookingStatus.PENDING_PAYMENT)
                .build();

        /*
         * Snapshot each held seat into BookingItem.
         */
        for (SeatHoldItem holdItem : hold.getItems()) {

            Seat seat =
                    holdItem.getShowSeat().getSeat();

            BookingItem bookingItem =
                    BookingItem.builder()
                            .booking(booking)
                            .showSeat(
                                    holdItem.getShowSeat()
                            )
                            .seatLabel(
                                    seat.getRowLabel()
                                            + seat.getSeatNumber()
                            )
                            .seatType(
                                    seat.getSeatType()
                            )
                            .price(
                                    holdItem.getPrice()
                            )
                            .build();

            booking.getItems().add(bookingItem);
        }

        Booking savedBooking =
                bookingRepository.save(booking);

        return bookingMapper.toResponse(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBooking(Long bookingId) {

        User user = currentUserService.getCurrentUser();

        Booking booking =
                bookingRepository.findByIdAndUserId(
                                bookingId,
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new ApplicationException(
                                        ErrorCode.RESOURCE_NOT_FOUND,
                                        "Booking not found with id: "
                                                + bookingId
                                )
                        );

        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings() {

        User user = currentUserService.getCurrentUser();

        return bookingRepository
                .findByUserId(user.getId())
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    private void validateHoldForBooking(
            SeatHold hold
    ) {

        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new ApplicationException(
                    ErrorCode.INVALID_STATE,
                    "Only an active seat hold can create a booking"
            );
        }

        if (!hold.getExpiresAt()
                .isAfter(LocalDateTime.now())) {

            throw new ApplicationException(
                    ErrorCode.HOLD_EXPIRED,
                    "Seat hold has expired"
            );
        }

        if (hold.getItems() == null
                || hold.getItems().isEmpty()) {

            throw new ApplicationException(
                    ErrorCode.INVALID_STATE,
                    "Seat hold contains no seats"
            );
        }
    }

    private String generateBookingReference() {

        return "DMG-"
                + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }
}
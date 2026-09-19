package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.CreatePaymentRequest;
import com.dmg.movieticket.dto.response.PaymentResponse;
import com.dmg.movieticket.entity.Booking;
import com.dmg.movieticket.entity.BookingStatus;
import com.dmg.movieticket.entity.HoldStatus;
import com.dmg.movieticket.entity.Payment;
import com.dmg.movieticket.entity.PaymentStatus;
import com.dmg.movieticket.entity.SeatHold;
import com.dmg.movieticket.entity.ShowSeat;
import com.dmg.movieticket.entity.ShowSeatStatus;
import com.dmg.movieticket.entity.User;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.mapper.PaymentMapper;
import com.dmg.movieticket.repository.BookingRepository;
import com.dmg.movieticket.repository.PaymentRepository;
import com.dmg.movieticket.repository.ShowSeatRepository;
import com.dmg.movieticket.security.CurrentUserService;
import com.dmg.movieticket.service.PaymentService;
import com.dmg.movieticket.service.payment.PaymentProcessingResult;
import com.dmg.movieticket.service.payment.PaymentProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final ShowSeatRepository showSeatRepository;
    private final PaymentProcessor paymentProcessor;
    private final PaymentMapper paymentMapper;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public PaymentResponse processPayment(
            Long bookingId,
            String idempotencyKey,
            CreatePaymentRequest request
    ) {

        /*
         * Idempotency key is mandatory.
         */
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "Idempotency-Key is required"
            );
        }

        String normalizedIdempotencyKey = idempotencyKey.trim();

        /*
         * If the exact same payment request is retried,
         * return the existing result instead of processing again.
         */
        Payment existingPayment = paymentRepository
                .findByIdempotencyKey(normalizedIdempotencyKey)
                .orElse(null);

        if (existingPayment != null) {

            User currentUser = currentUserService.getCurrentUser();

            if (!existingPayment.getBooking()
                    .getUser()
                    .getId()
                    .equals(currentUser.getId())) {

                throw new ApplicationException(
                        ErrorCode.INVALID_REQUEST,
                        "Invalid idempotency key"
                );
            }

            /*
             * Important:
             * The idempotency key must belong to this booking too.
             */
            if (!existingPayment.getBooking()
                    .getId()
                    .equals(bookingId)) {

                throw new ApplicationException(
                        ErrorCode.INVALID_REQUEST,
                        "Idempotency key belongs to a different booking"
                );
            }

            return paymentMapper.toResponse(existingPayment);
        }

        User user = currentUserService.getCurrentUser();

        /*
         * Lock booking row.
         *
         * This prevents two concurrent payment requests from
         * confirming the same booking simultaneously.
         */
        Booking booking = bookingRepository
                .findByIdAndUserIdForUpdate(
                        bookingId,
                        user.getId()
                )
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Booking not found with id: " + bookingId
                ));

        validateBookingForPayment(booking);

        SeatHold hold = booking.getSeatHold();

        LocalDateTime now = LocalDateTime.now();

        /*
         * Do NOT change booking status here and then throw.
         *
         * Since this method is @Transactional, throwing a runtime
         * exception would normally roll back that state update.
         *
         * Hold-expiry cleanup will update:
         *
         * SeatHold ACTIVE -> EXPIRED
         * Booking PENDING_PAYMENT -> EXPIRED
         * ShowSeat HELD -> AVAILABLE
         */
        if (hold.getStatus() != HoldStatus.ACTIVE
                || !hold.getExpiresAt().isAfter(now)) {

            throw new ApplicationException(
                    ErrorCode.HOLD_EXPIRED,
                    "Seat hold has expired"
            );
        }

        /*
         * Collect the seats belonging to this booking.
         */
        List<Long> showSeatIds = booking.getItems()
                .stream()
                .map(item -> item.getShowSeat().getId())
                .distinct()
                .sorted()
                .toList();

        if (showSeatIds.isEmpty()) {
            throw new ApplicationException(
                    ErrorCode.INVALID_STATE,
                    "Booking contains no seats"
            );
        }

        /*
         * Lock all ShowSeat rows before changing them.
         */
        List<ShowSeat> showSeats =
                showSeatRepository.findByIdsForUpdate(showSeatIds);

        if (showSeats.size() != showSeatIds.size()) {
            throw new ApplicationException(
                    ErrorCode.INVALID_STATE,
                    "One or more booking seats no longer exist"
            );
        }

        /*
         * Every seat must still be HELD.
         */
        for (ShowSeat showSeat : showSeats) {

            if (showSeat.getStatus() != ShowSeatStatus.HELD) {
                throw new ApplicationException(
                        ErrorCode.INVALID_STATE,
                        "Seat is no longer held: "
                                + getSeatLabel(showSeat)
                );
            }
        }

        /*
         * Create the payment attempt.
         */
        Payment payment = Payment.builder()
                .booking(booking)
                .idempotencyKey(normalizedIdempotencyKey)
                .amount(booking.getTotalAmount())
                .paymentMethod(request.paymentMethod())
                .status(PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);

        /*
         * External payment-provider abstraction.
         *
         * Current implementation can be a mock processor.
         */
        PaymentProcessingResult result =
                paymentProcessor.process(
                        booking.getTotalAmount(),
                        request.paymentMethod()
                );

        /*
         * Payment failed.
         *
         * Booking stays PENDING_PAYMENT.
         * Hold stays ACTIVE.
         * Seats stay HELD.
         *
         * The customer may retry payment while the hold is valid.
         */
        if (!result.successful()) {

            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(result.failureReason());

            Payment savedPayment =
                    paymentRepository.save(payment);

            return paymentMapper.toResponse(savedPayment);
        }

        /*
         * Payment succeeded.
         */
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setProviderReference(
                result.providerReference()
        );

        /*
         * Convert the temporary reservation into a confirmed booking.
         *
         * All these updates happen in the SAME transaction.
         */
        for (ShowSeat showSeat : showSeats) {
            showSeat.setStatus(ShowSeatStatus.BOOKED);
        }

        hold.setStatus(HoldStatus.CONFIRMED);

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(now);

        /*
         * Explicit saves.
         *
         * Booking owns SeatHold through the object graph, but keeping
         * these state changes clear makes the transaction easier to read.
         */
        showSeatRepository.saveAll(showSeats);

        bookingRepository.save(booking);

        Payment savedPayment =
                paymentRepository.save(payment);

        return paymentMapper.toResponse(savedPayment);
    }

    private void validateBookingForPayment(Booking booking) {

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new ApplicationException(
                    ErrorCode.INVALID_STATE,
                    "Booking is already confirmed"
            );
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ApplicationException(
                    ErrorCode.INVALID_STATE,
                    "Cancelled booking cannot be paid"
            );
        }

        if (booking.getStatus() == BookingStatus.EXPIRED) {
            throw new ApplicationException(
                    ErrorCode.HOLD_EXPIRED,
                    "Booking has expired"
            );
        }

        if (booking.getStatus()
                != BookingStatus.PENDING_PAYMENT) {

            throw new ApplicationException(
                    ErrorCode.INVALID_STATE,
                    "Booking is not eligible for payment"
            );
        }
    }

    private String getSeatLabel(ShowSeat showSeat) {

        return showSeat.getSeat().getRowLabel()
                + showSeat.getSeat().getSeatNumber();
    }
}
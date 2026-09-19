package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.response.BookingCancellationResponse;
import com.dmg.movieticket.dto.response.RefundResponse;
import com.dmg.movieticket.entity.*;
import com.dmg.movieticket.event.BookingCancelledEvent;
import com.dmg.movieticket.event.RefundProcessedEvent;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.mapper.RefundMapper;
import com.dmg.movieticket.repository.*;
import com.dmg.movieticket.security.CurrentUserService;
import com.dmg.movieticket.service.RefundService;
import com.dmg.movieticket.service.refund.RefundProcessingResult;
import com.dmg.movieticket.service.refund.RefundProcessor;
import com.dmg.movieticket.strategy.refund.RefundCalculationResult;
import com.dmg.movieticket.strategy.refund.RefundCalculationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final ShowSeatRepository showSeatRepository;

    private final RefundCalculationStrategy refundCalculationStrategy;
    private final RefundProcessor refundProcessor;
    private final RefundMapper refundMapper;

    private final CurrentUserService currentUserService;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public BookingCancellationResponse cancelBooking(
            Long bookingId
    ) {

        User user = currentUserService.getCurrentUser();

        Booking booking =
                bookingRepository.findByIdAndUserIdForUpdate(
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

        validateBookingForCancellation(booking);

        if (refundRepository.existsByBookingId(bookingId)) {
            throw new ApplicationException(
                    ErrorCode.INVALID_STATE,
                    "Refund already exists for this booking"
            );
        }

        Payment payment =
                paymentRepository
                        .findFirstByBookingIdAndStatusOrderByCreatedAtDesc(
                                bookingId,
                                PaymentStatus.SUCCESS
                        )
                        .orElseThrow(() ->
                                new ApplicationException(
                                        ErrorCode.INVALID_STATE,
                                        "Successful payment not found for booking"
                                )
                        );

        LocalDateTime now = LocalDateTime.now();

        RefundCalculationResult calculation =
                refundCalculationStrategy.calculate(
                        booking,
                        now
                );

        List<Long> showSeatIds =
                booking.getItems()
                        .stream()
                        .map(item -> item.getShowSeat().getId())
                        .distinct()
                        .sorted()
                        .toList();

        List<ShowSeat> showSeats =
                showSeatRepository.findByIdsForUpdate(
                        showSeatIds
                );

        if (showSeats.size() != showSeatIds.size()) {
            throw new ApplicationException(
                    ErrorCode.INVALID_STATE,
                    "One or more booking seats no longer exist"
            );
        }

        for (ShowSeat showSeat : showSeats) {

            if (showSeat.getStatus()
                    != ShowSeatStatus.BOOKED) {

                throw new ApplicationException(
                        ErrorCode.INVALID_STATE,
                        "Invalid seat state during cancellation"
                );
            }
        }

        Refund refund = Refund.builder()
                .booking(booking)
                .payment(payment)
                .amount(calculation.refundAmount())
                .refundPercentage(
                        calculation.refundPercentage()
                )
                .status(RefundStatus.PENDING)
                .build();

        refund = refundRepository.save(refund);

        /*
         * If refund amount is zero, there is no external
         * refund request to process.
         */
        if (calculation.refundAmount()
                .compareTo(BigDecimal.ZERO) > 0) {

            RefundProcessingResult result =
                    refundProcessor.processRefund(
                            payment.getProviderReference(),
                            calculation.refundAmount()
                    );

            if (!result.successful()) {

                refund.setStatus(RefundStatus.FAILED);

                refundRepository.save(refund);

                throw new ApplicationException(
                        ErrorCode.INVALID_STATE,
                        "Refund processing failed"
                );
            }

            refund.setStatus(RefundStatus.SUCCESS);
            refund.setProviderReference(
                    result.providerReference()
            );
            refund.setProcessedAt(now);

        } else {

            /*
             * No monetary refund is due,
             * but cancellation itself is valid.
             */
            refund.setStatus(RefundStatus.SUCCESS);
            refund.setProcessedAt(now);
        }

        /*
         * Release booked seats for resale.
         */
        for (ShowSeat showSeat : showSeats) {
            showSeat.setStatus(
                    ShowSeatStatus.AVAILABLE
            );
        }

        booking.setStatus(
                BookingStatus.CANCELLED
        );

        booking.setCancelledAt(now);

        /*
         * We mark payment REFUNDED when an actual
         * monetary refund occurred.
         */
        if (calculation.refundAmount()
                .compareTo(BigDecimal.ZERO) > 0) {

            payment.setStatus(
                    PaymentStatus.REFUNDED
            );
        }

        showSeatRepository.saveAll(showSeats);
        bookingRepository.save(booking);
        paymentRepository.save(payment);

        Refund savedRefund =
                refundRepository.save(refund);

        /*
         * These events will only be handled after the
         * cancellation transaction commits successfully.
         */
        eventPublisher.publishEvent(
                new BookingCancelledEvent(
                        booking.getId()
                )
        );

        eventPublisher.publishEvent(
                new RefundProcessedEvent(
                        savedRefund.getId()
                )
        );

        RefundResponse refundResponse =
                refundMapper.toResponse(savedRefund);

        return new BookingCancellationResponse(
                booking.getId(),
                booking.getBookingReference(),
                booking.getStatus(),
                refundResponse
        );
    }

    private void validateBookingForCancellation(
            Booking booking
    ) {

        if (booking.getStatus()
                != BookingStatus.CONFIRMED) {

            throw new ApplicationException(
                    ErrorCode.CANCELLATION_NOT_ALLOWED,
                    "Only confirmed bookings can be cancelled"
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        if (!booking.getShow()
                .getStartTime()
                .isAfter(now)) {

            throw new ApplicationException(
                    ErrorCode.CANCELLATION_NOT_ALLOWED,
                    "Booking cannot be cancelled after the show has started"
            );
        }
    }
}
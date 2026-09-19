package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.entity.*;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.repository.BookingRepository;
import com.dmg.movieticket.repository.NotificationRepository;
import com.dmg.movieticket.repository.RefundRepository;
import com.dmg.movieticket.resolver.NotificationStrategyResolver;
import com.dmg.movieticket.service.NotificationService;
import com.dmg.movieticket.strategy.notification.NotificationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final BookingRepository bookingRepository;
    private final RefundRepository refundRepository;
    private final NotificationStrategyResolver strategyResolver;

    @Override
    @Transactional
    public void sendBookingConfirmation(Long bookingId) {

        Booking booking = getBooking(bookingId);

        String message =
                "Booking confirmed. Reference: "
                        + booking.getBookingReference()
                        + ", Movie: "
                        + booking.getShow().getMovie().getTitle()
                        + ", Show time: "
                        + booking.getShow().getStartTime();

        createAndSend(
                booking.getUser(),
                booking,
                NotificationChannel.EMAIL,
                message
        );
    }

    @Override
    @Transactional
    public void scheduleBookingReminder(Long bookingId) {

        Booking booking = getBooking(bookingId);

        LocalDateTime scheduledAt =
                booking.getShow()
                        .getStartTime()
                        .minusHours(2);

        /*
         * If the booking is confirmed less than 2 hours
         * before the show, schedule the reminder immediately.
         */
        if (scheduledAt.isBefore(LocalDateTime.now())) {
            scheduledAt = LocalDateTime.now();
        }

        String message =
                "Reminder for booking "
                        + booking.getBookingReference()
                        + ". "
                        + booking.getShow().getMovie().getTitle()
                        + " starts at "
                        + booking.getShow().getStartTime();

        Notification notification =
                Notification.builder()
                        .user(booking.getUser())
                        .booking(booking)
                        .channel(NotificationChannel.EMAIL)
                        .status(NotificationStatus.PENDING)
                        .message(message)
                        .scheduledAt(scheduledAt)
                        .build();

        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void sendBookingCancellation(Long bookingId) {

        Booking booking = getBooking(bookingId);

        String message =
                "Booking cancelled. Reference: "
                        + booking.getBookingReference();

        createAndSend(
                booking.getUser(),
                booking,
                NotificationChannel.EMAIL,
                message
        );
    }

    @Override
    @Transactional
    public void sendRefundProcessed(Long refundId) {

        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() ->
                        new ApplicationException(
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "Refund not found with id: "
                                        + refundId
                        )
                );

        Booking booking = refund.getBooking();

        String message =
                "Refund processed for booking "
                        + booking.getBookingReference()
                        + ". Refund amount: "
                        + refund.getAmount();

        createAndSend(
                booking.getUser(),
                booking,
                NotificationChannel.EMAIL,
                message
        );
    }

    @Override
    @Transactional
    public void processPendingNotifications() {

        LocalDateTime now = LocalDateTime.now();

        List<Notification> notifications =
                notificationRepository
                        .findByStatusAndScheduledAtLessThanEqual(
                                NotificationStatus.PENDING,
                                now
                        );

        for (Notification notification : notifications) {

            /*
             * Our only scheduled PENDING notifications currently
             * are booking reminders.
             *
             * If the booking was cancelled after the reminder was
             * scheduled, don't send it.
             */
            if (notification.getBooking() != null
                    && notification.getBooking().getStatus()
                    != BookingStatus.CONFIRMED) {

                notification.setStatus(
                        NotificationStatus.FAILED
                );

                notification.setFailureReason(
                        "Booking is no longer confirmed"
                );

                notificationRepository.save(notification);

                continue;
            }

            sendExistingNotification(notification);
        }
    }

    private void createAndSend(
            User user,
            Booking booking,
            NotificationChannel channel,
            String message
    ) {

        Notification notification =
                Notification.builder()
                        .user(user)
                        .booking(booking)
                        .channel(channel)
                        .status(NotificationStatus.PENDING)
                        .message(message)
                        .scheduledAt(LocalDateTime.now())
                        .build();

        notification =
                notificationRepository.save(notification);

        sendExistingNotification(notification);
    }

    private void sendExistingNotification(
            Notification notification
    ) {

        try {

            NotificationStrategy strategy =
                    strategyResolver.resolve(
                            notification.getChannel()
                    );

            String recipient =
                    notification.getUser().getEmail();

            strategy.send(
                    recipient,
                    notification.getMessage()
            );

            notification.setStatus(
                    NotificationStatus.SENT
            );

            notification.setSentAt(
                    LocalDateTime.now()
            );

            notification.setFailureReason(null);

        } catch (Exception exception) {

            notification.setStatus(
                    NotificationStatus.FAILED
            );

            notification.setFailureReason(
                    exception.getMessage()
            );
        }

        notificationRepository.save(notification);
    }

    private Booking getBooking(Long bookingId) {

        return bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ApplicationException(
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "Booking not found with id: "
                                        + bookingId
                        )
                );
    }
}
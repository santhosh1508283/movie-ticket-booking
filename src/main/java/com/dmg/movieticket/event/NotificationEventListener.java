package com.dmg.movieticket.event;

import com.dmg.movieticket.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleBookingConfirmed(
            BookingConfirmedEvent event
    ) {

        log.info(
                "BookingConfirmedEvent received for bookingId={}",
                event.bookingId()
        );

        notificationService.sendBookingConfirmation(
                event.bookingId()
        );

        notificationService.scheduleBookingReminder(
                event.bookingId()
        );
    }

    @Async
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleBookingCancelled(
            BookingCancelledEvent event
    ) {

        log.info(
                "BookingCancelledEvent received for bookingId={}",
                event.bookingId()
        );

        notificationService.sendBookingCancellation(
                event.bookingId()
        );
    }

    @Async
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleRefundProcessed(
            RefundProcessedEvent event
    ) {

        log.info(
                "RefundProcessedEvent received for bookingId={}",
                event.refundId()
        );

        notificationService.sendRefundProcessed(
                event.refundId()
        );
    }
}
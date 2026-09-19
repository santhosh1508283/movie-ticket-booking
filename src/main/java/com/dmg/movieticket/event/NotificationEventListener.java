package com.dmg.movieticket.event;

import com.dmg.movieticket.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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

        notificationService.sendRefundProcessed(
                event.refundId()
        );
    }
}
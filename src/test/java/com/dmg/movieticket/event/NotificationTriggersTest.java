package com.dmg.movieticket.event;

import com.dmg.movieticket.scheduler.*;
import com.dmg.movieticket.service.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class NotificationTriggersTest {
    NotificationService notifications = mock(NotificationService.class);
    NotificationEventListener listener = new NotificationEventListener(notifications);
    @Test void confirmationTriggersConfirmationAndReminder() {
        listener.handleBookingConfirmed(new BookingConfirmedEvent(8L));
        verify(notifications).sendBookingConfirmation(8L); verify(notifications).scheduleBookingReminder(8L);
        verifyNoMoreInteractions(notifications);
    }
    @Test void cancellationTriggersCancellationNotice() {
        listener.handleBookingCancelled(new BookingCancelledEvent(8L));
        verify(notifications).sendBookingCancellation(8L); verifyNoMoreInteractions(notifications);
    }
    @Test void refundTriggersRefundNotice() {
        listener.handleRefundProcessed(new RefundProcessedEvent(11L));
        verify(notifications).sendRefundProcessed(11L); verifyNoMoreInteractions(notifications);
    }
    @Test void notificationSchedulerProcessesPendingMessages() {
        new NotificationScheduler(notifications).processNotifications(); verify(notifications).processPendingNotifications();
    }
    @Test void holdSchedulerRunsExpiration() {
        SeatHoldService holds = mock(SeatHoldService.class);
        new SeatHoldExpirationScheduler(holds).expireSeatHolds(); verify(holds).expireExpiredHolds();
    }
}

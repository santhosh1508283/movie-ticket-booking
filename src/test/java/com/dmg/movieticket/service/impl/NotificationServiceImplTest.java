package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.entity.*;
import com.dmg.movieticket.repository.*;
import com.dmg.movieticket.resolver.NotificationStrategyResolver;
import com.dmg.movieticket.strategy.notification.NotificationStrategy;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import java.time.LocalDateTime;
import java.util.*;
import static com.dmg.movieticket.support.TestFixtures.*;
import static com.dmg.movieticket.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationServiceImplTest {
    NotificationRepository notifications = mock(NotificationRepository.class);
    BookingRepository bookings = mock(BookingRepository.class);
    RefundRepository refunds = mock(RefundRepository.class);
    NotificationStrategyResolver resolver = mock(NotificationStrategyResolver.class);
    NotificationStrategy sender = mock(NotificationStrategy.class);
    NotificationServiceImpl service = new NotificationServiceImpl(notifications, bookings, refunds, resolver);
    Booking booking;
    @BeforeEach void setUp() {
        booking = booking(); booking.setStatus(BookingStatus.CONFIRMED);
        when(bookings.findById(8L)).thenReturn(Optional.of(booking));
        when(notifications.save(any())).thenAnswer(i -> i.getArgument(0));
        when(resolver.resolve(NotificationChannel.EMAIL)).thenReturn(sender);
    }
    Notification lastSaved() {
        ArgumentCaptor<Notification> captured = ArgumentCaptor.forClass(Notification.class);
        verify(notifications, atLeastOnce()).save(captured.capture()); return captured.getValue();
    }
    @Test void confirmationSendsToBookingOwnerAndRecordsSuccess() {
        service.sendBookingConfirmation(8L);
        Notification saved = lastSaved();
        assertEquals(NotificationStatus.SENT, saved.getStatus()); assertNotNull(saved.getSentAt());
        assertNull(saved.getFailureReason()); assertTrue(saved.getMessage().contains("Interstellar"));
        verify(sender).send(eq("customer@example.test"), contains("DMG-TEST1234"));
    }
    @Test void cancellationSendsReference() {
        service.sendBookingCancellation(8L); verify(sender).send(eq("customer@example.test"), contains("Booking cancelled"));
    }
    @Test void refundNotificationIncludesAmount() {
        when(refunds.findById(11L)).thenReturn(Optional.of(Refund.builder().booking(booking).amount(money("75.00")).build()));
        service.sendRefundProcessed(11L); verify(sender).send(eq("customer@example.test"), contains("75.00"));
    }
    @Test void deliveryFailureIsRecordedWithoutThrowing() {
        doThrow(new IllegalStateException("SMTP unavailable")).when(sender).send(any(), any());
        assertDoesNotThrow(() -> service.sendBookingConfirmation(8L));
        assertEquals(NotificationStatus.FAILED, lastSaved().getStatus());
        assertEquals("SMTP unavailable", lastSaved().getFailureReason());
    }
    @Test void schedulesReminderTwoHoursBeforeShowWithoutSending() {
        service.scheduleBookingReminder(8L);
        assertEquals(booking.getShow().getStartTime().minusHours(2), lastSaved().getScheduledAt());
        assertEquals(NotificationStatus.PENDING, lastSaved().getStatus()); verifyNoInteractions(sender);
    }
    @Test void nearShowReminderIsScheduledImmediately() {
        booking.getShow().setStartTime(LocalDateTime.now().plusMinutes(30));
        LocalDateTime before = LocalDateTime.now(); service.scheduleBookingReminder(8L);
        assertFalse(lastSaved().getScheduledAt().isBefore(before));
        assertFalse(lastSaved().getScheduledAt().isAfter(LocalDateTime.now()));
    }
    @Test void pendingCancelledBookingReminderIsNotSent() {
        booking.setStatus(BookingStatus.CANCELLED);
        Notification reminder = reminder();
        when(notifications.findByStatusAndScheduledAtLessThanEqual(eq(NotificationStatus.PENDING), any())).thenReturn(List.of(reminder));
        service.processPendingNotifications();
        assertEquals(NotificationStatus.FAILED, reminder.getStatus()); verifyNoInteractions(sender);
    }
    @Test void pendingConfirmedReminderIsSentAndFailureReasonCleared() {
        Notification reminder = reminder(); reminder.setFailureReason("old error");
        when(notifications.findByStatusAndScheduledAtLessThanEqual(eq(NotificationStatus.PENDING), any())).thenReturn(List.of(reminder));
        service.processPendingNotifications();
        assertEquals(NotificationStatus.SENT, reminder.getStatus()); assertNull(reminder.getFailureReason());
    }
    @Test void failureDoesNotPreventLaterNotificationDelivery() {
        Notification first = reminder(), second = reminder();
        first.setMessage("first"); second.setMessage("second");
        doThrow(new IllegalStateException("failed")).when(sender).send(any(), eq("first"));
        when(notifications.findByStatusAndScheduledAtLessThanEqual(eq(NotificationStatus.PENDING), any())).thenReturn(List.of(first, second));
        service.processPendingNotifications();
        assertEquals(NotificationStatus.FAILED, first.getStatus()); assertEquals(NotificationStatus.SENT, second.getStatus());
    }
    @Test void rejectsMissingBookingAndRefund() {
        error(RESOURCE_NOT_FOUND, () -> service.sendBookingConfirmation(99L));
        error(RESOURCE_NOT_FOUND, () -> service.scheduleBookingReminder(99L));
        error(RESOURCE_NOT_FOUND, () -> service.sendBookingCancellation(99L));
        error(RESOURCE_NOT_FOUND, () -> service.sendRefundProcessed(99L));
    }
    Notification reminder() {
        return Notification.builder().booking(booking).user(booking.getUser()).channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.PENDING).message("Reminder").scheduledAt(LocalDateTime.now().minusMinutes(1)).build();
    }
}

package com.dmg.movieticket.service;

public interface NotificationService {

    void sendBookingConfirmation(Long bookingId);

    void scheduleBookingReminder(Long bookingId);

    void sendBookingCancellation(Long bookingId);

    void sendRefundProcessed(Long refundId);

    void processPendingNotifications();
}
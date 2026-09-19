package com.dmg.movieticket.strategy.notification;

import com.dmg.movieticket.entity.NotificationChannel;

public interface NotificationStrategy {

    boolean supports(NotificationChannel channel);

    void send(
            String recipient,
            String message
    );
}
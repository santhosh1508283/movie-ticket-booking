package com.dmg.movieticket.strategy.notification;

import com.dmg.movieticket.entity.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationStrategy
        implements NotificationStrategy {

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    @Override
    public void send(
            String recipient,
            String message
    ) {

        log.info(
                "Mock email sent to {}: {}",
                recipient,
                message
        );
    }
}
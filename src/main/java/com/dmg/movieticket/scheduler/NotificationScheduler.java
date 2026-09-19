package com.dmg.movieticket.scheduler;

import com.dmg.movieticket.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationService notificationService;

    @Scheduled(
            fixedDelayString =
                    "${notification.processing-interval-ms:60000}"
    )
    public void processNotifications() {

        notificationService
                .processPendingNotifications();
    }
}
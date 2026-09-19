package com.dmg.movieticket.resolver;

import com.dmg.movieticket.entity.NotificationChannel;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.strategy.notification.NotificationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationStrategyResolver {

    private final List<NotificationStrategy> strategies;

    public NotificationStrategy resolve(
            NotificationChannel channel
    ) {

        return strategies.stream()
                .filter(strategy ->
                        strategy.supports(channel)
                )
                .findFirst()
                .orElseThrow(() ->
                        new ApplicationException(
                                ErrorCode.INVALID_STATE,
                                "Notification channel is not supported: "
                                        + channel
                        )
                );
    }
}
package com.dmg.movieticket.strategy;

import com.dmg.movieticket.entity.NotificationChannel;
import com.dmg.movieticket.resolver.NotificationStrategyResolver;
import com.dmg.movieticket.strategy.notification.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.List;
import static com.dmg.movieticket.support.TestFixtures.*;
import static com.dmg.movieticket.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;

class NotificationStrategyResolverTest {
    EmailNotificationStrategy email = new EmailNotificationStrategy();
    NotificationStrategyResolver resolver = new NotificationStrategyResolver(List.of(email));
    @Test void selectsEmailStrategy() {
        assertTrue(email.supports(NotificationChannel.EMAIL)); assertSame(email, resolver.resolve(NotificationChannel.EMAIL));
        assertDoesNotThrow(() -> email.send("customer@example.test", "Test notification"));
    }
    @ParameterizedTest @EnumSource(value = NotificationChannel.class, names = "EMAIL", mode = EnumSource.Mode.EXCLUDE)
    void rejectsUnsupportedChannels(NotificationChannel channel) {
        assertFalse(email.supports(channel)); error(INVALID_STATE, () -> resolver.resolve(channel));
    }
    @Test void rejectsEmptyRegistry() {
        error(INVALID_STATE, () -> new NotificationStrategyResolver(List.of()).resolve(NotificationChannel.EMAIL));
    }
}

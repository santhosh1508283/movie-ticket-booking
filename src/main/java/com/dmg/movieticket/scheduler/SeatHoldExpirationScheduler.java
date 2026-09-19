package com.dmg.movieticket.scheduler;

import com.dmg.movieticket.service.SeatHoldService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeatHoldExpirationScheduler {

    private final SeatHoldService seatHoldService;

    @Scheduled(
            fixedDelayString =
                    "${booking.seat-hold-cleanup-interval-ms:30000}"
    )
    public void expireSeatHolds() {
        seatHoldService.expireExpiredHolds();
    }
}
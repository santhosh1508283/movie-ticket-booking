package com.dmg.movieticket.repository;

import com.dmg.movieticket.entity.HoldStatus;
import com.dmg.movieticket.entity.SeatHold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {

    Optional<SeatHold> findByIdAndUserId(
            Long id,
            Long userId
    );

    List<SeatHold> findByUserIdAndStatus(
            Long userId,
            HoldStatus status
    );

    List<SeatHold> findByStatusAndExpiresAtBefore(
            HoldStatus status,
            LocalDateTime time
    );

    boolean existsByIdAndUserIdAndStatus(
            Long id,
            Long userId,
            HoldStatus status
    );
}
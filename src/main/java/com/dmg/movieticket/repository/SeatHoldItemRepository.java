package com.dmg.movieticket.repository;

import com.dmg.movieticket.entity.SeatHoldItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeatHoldItemRepository extends JpaRepository<SeatHoldItem, Long> {

    List<SeatHoldItem> findBySeatHoldId(Long seatHoldId);

    Optional<SeatHoldItem> findBySeatHoldIdAndShowSeatId(
            Long seatHoldId,
            Long showSeatId
    );
}
package com.dmg.movieticket.repository;

import com.dmg.movieticket.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByScreenId(Long screenId);

    Optional<Seat> findByScreenIdAndRowLabelIgnoreCaseAndSeatNumber(
            Long screenId,
            String rowLabel,
            Integer seatNumber
    );

    boolean existsByScreenIdAndRowLabelIgnoreCaseAndSeatNumber(
            Long screenId,
            String rowLabel,
            Integer seatNumber
    );
}
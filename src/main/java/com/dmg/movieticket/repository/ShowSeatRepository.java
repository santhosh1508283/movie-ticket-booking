package com.dmg.movieticket.repository;

import com.dmg.movieticket.entity.ShowSeat;
import com.dmg.movieticket.entity.ShowSeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    List<ShowSeat> findByShowId(Long showId);

    List<ShowSeat> findByShowIdAndStatus(
            Long showId,
            ShowSeatStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ss
            FROM ShowSeat ss
            WHERE ss.show.id = :showId
              AND ss.id IN :showSeatIds
            ORDER BY ss.id
            """)
    List<ShowSeat> findByShowIdAndIdsForUpdate(
            @Param("showId") Long showId,
            @Param("showSeatIds") List<Long> showSeatIds
    );
}
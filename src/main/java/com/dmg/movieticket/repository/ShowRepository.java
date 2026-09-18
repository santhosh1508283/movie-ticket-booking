package com.dmg.movieticket.repository;

import com.dmg.movieticket.entity.Show;
import com.dmg.movieticket.entity.ShowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

    List<Show> findByMovieId(Long movieId);

    List<Show> findByScreenId(Long screenId);

    List<Show> findByMovieIdAndStartTimeBetween(
            Long movieId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Show> findByScreenIdAndStartTimeBetween(
            Long screenId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Show> findByStatus(ShowStatus status);

    @Query("""
        SELECT s
        FROM Show s
        WHERE s.screen.id = :screenId
          AND s.status = com.dmg.movieticket.entity.ShowStatus.SCHEDULED
          AND s.startTime < :newEndTime
          AND s.endTime > :newStartTime
        """)
    List<Show> findOverlappingShows(
            @Param("screenId") Long screenId,
            @Param("newStartTime") LocalDateTime newStartTime,
            @Param("newEndTime") LocalDateTime newEndTime
    );
}
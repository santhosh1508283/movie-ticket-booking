package com.dmg.movieticket.repository;

import com.dmg.movieticket.entity.Booking;
import com.dmg.movieticket.entity.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    List<Booking> findByUserId(Long userId);

    List<Booking> findByUserIdAndStatus(
            Long userId,
            BookingStatus status
    );

    Optional<Booking> findByIdAndUserId(
            Long id,
            Long userId
    );

    boolean existsBySeatHoldId(Long seatHoldId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b
        FROM Booking b
        WHERE b.id = :bookingId
          AND b.user.id = :userId
        """)
    Optional<Booking> findByIdAndUserIdForUpdate(
            @Param("bookingId") Long bookingId,
            @Param("userId") Long userId
    );

    Optional<Booking> findBySeatHoldId(Long seatHoldId);
}
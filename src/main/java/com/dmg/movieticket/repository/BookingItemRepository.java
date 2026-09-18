package com.dmg.movieticket.repository;

import com.dmg.movieticket.entity.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {

    List<BookingItem> findByBookingId(Long bookingId);

    boolean existsByBookingIdAndShowSeatId(
            Long bookingId,
            Long showSeatId
    );
}
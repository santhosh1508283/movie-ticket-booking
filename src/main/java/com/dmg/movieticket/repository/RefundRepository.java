package com.dmg.movieticket.repository;

import com.dmg.movieticket.entity.Refund;
import com.dmg.movieticket.entity.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    List<Refund> findByStatus(RefundStatus status);
}
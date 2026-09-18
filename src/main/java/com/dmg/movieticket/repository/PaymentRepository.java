package com.dmg.movieticket.repository;

import com.dmg.movieticket.entity.Payment;
import com.dmg.movieticket.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findFirstByBookingIdAndStatusOrderByCreatedAtDesc(
            Long bookingId,
            PaymentStatus status
    );

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findTopByBookingIdAndStatusOrderByCreatedAtDesc(
            Long bookingId,
            PaymentStatus status
    );
}
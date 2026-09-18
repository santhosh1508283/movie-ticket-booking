package com.dmg.movieticket.repository;

import com.dmg.movieticket.entity.Notification;
import com.dmg.movieticket.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByBookingId(Long bookingId);

    List<Notification> findByStatusAndScheduledAtLessThanEqual(
            NotificationStatus status,
            LocalDateTime scheduledAt
    );

    List<Notification> findByStatus(NotificationStatus status);
}
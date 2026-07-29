package com.sagant.notifications.repository;

import com.sagant.notifications.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query(value = """
            SELECT * FROM notifications
            WHERE status = 'PENDING' AND next_attempt_at <= :now
            ORDER BY
                CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END ASC,
                created_at ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Notification> findBatchForDispatch(@Param("now") Instant now, @Param("batchSize") int batchSize);
}
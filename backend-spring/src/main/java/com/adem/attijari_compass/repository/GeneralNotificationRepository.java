package com.adem.attijari_compass.repository;

import com.adem.attijari_compass.entity.GeneralNotification;
import com.adem.attijari_compass.entity.NotificationTargetRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface GeneralNotificationRepository extends JpaRepository<GeneralNotification, Long> {
    List<GeneralNotification> findAllByOrderByCreatedAtDesc();

    @Query("""
            SELECT n
            FROM GeneralNotification n
            WHERE n.active = true
              AND n.targetRole IN :roles
              AND n.publishedAt IS NOT NULL
              AND n.publishedAt <= :now
              AND (n.expiresAt IS NULL OR n.expiresAt > :now)
            ORDER BY n.publishedAt DESC, n.createdAt DESC
            """)
    List<GeneralNotification> findVisibleForRole(@Param("roles") List<NotificationTargetRole> roles,
                                                 @Param("now") LocalDateTime now);
}

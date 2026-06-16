package com.adem.attijari_compass.dto.admin;

import com.adem.attijari_compass.entity.GeneralNotificationType;
import com.adem.attijari_compass.entity.NotificationTargetRole;

import java.time.LocalDateTime;

public record GeneralNotificationDto(
        Long id,
        String title,
        String message,
        GeneralNotificationType type,
        NotificationTargetRole targetRole,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime publishedAt,
        LocalDateTime expiresAt
) {
}

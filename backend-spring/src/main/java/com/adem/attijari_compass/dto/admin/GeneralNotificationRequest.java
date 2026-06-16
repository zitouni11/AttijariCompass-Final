package com.adem.attijari_compass.dto.admin;

import com.adem.attijari_compass.entity.GeneralNotificationType;
import com.adem.attijari_compass.entity.NotificationTargetRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record GeneralNotificationRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 3000) String message,
        @NotNull GeneralNotificationType type,
        @NotNull NotificationTargetRole targetRole,
        LocalDateTime expiresAt
) {
}

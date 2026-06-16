package com.adem.attijari_compass.dto.admin;

import com.adem.attijari_compass.entity.AuditStatus;

import java.time.LocalDateTime;

public record AuditLogDto(
        Long id,
        Long actorId,
        String actorEmail,
        String actorRole,
        String action,
        String module,
        AuditStatus status,
        String message,
        LocalDateTime createdAt
) {
}

package com.adem.attijari_compass.dto.admin;

import java.time.LocalDateTime;

public record AccountRestoreRequestDto(
        Long id,
        String email,
        String fullName,
        boolean emailVerified,
        String status,
        LocalDateTime requestedAt,
        LocalDateTime verifiedAt,
        LocalDateTime approvedAt,
        Long approvedBy,
        LocalDateTime rejectedAt,
        Long rejectedBy,
        String rejectionReason
) {
}

package com.adem.attijari_compass.dto.admin;

import java.time.LocalDateTime;

public record AdminRegistrationResponseDto(
        Long id,
        String fullName,
        String email,
        String status,
        LocalDateTime createdAt,
        LocalDateTime verifiedAt,
        LocalDateTime reviewedAt,
        String reviewedByEmail,
        String rejectionReason
) {
}

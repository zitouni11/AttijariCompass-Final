package com.adem.attijari_compass.dto.admin;

import java.time.LocalDateTime;

public record AdminUserDto(
        Long id,
        String fullName,
        String email,
        String role,
        boolean active,
        boolean deleted,
        LocalDateTime deletedAt,
        String deletionReason,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
}

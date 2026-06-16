package com.adem.attijari_compass.dto.admin;

import com.adem.attijari_compass.entity.SupportTicketCategory;
import com.adem.attijari_compass.entity.SupportTicketStatus;

import java.time.LocalDateTime;

public record SupportTicketDto(
        Long id,
        Long userId,
        String userEmail,
        String subject,
        SupportTicketCategory category,
        String message,
        SupportTicketStatus status,
        String adminReply,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt
) {
}

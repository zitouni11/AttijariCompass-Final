package com.adem.attijari_compass.dto.admin;

import java.time.LocalDateTime;

public record TechnicalStatusDto(
        String backendStatus,
        String databaseStatus,
        String fastApiStatus,
        String chatbotStatus,
        String powerBiStatus,
        String uptime,
        long apiAverageResponseTime,
        LocalDateTime lastCheckedAt
) {
}

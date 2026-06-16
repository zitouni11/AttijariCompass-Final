package com.adem.attijari_compass.dto.admin;

import java.time.LocalDateTime;

public record AppSettingDto(
        Long id,
        String settingKey,
        String settingValue,
        String type,
        String description,
        LocalDateTime updatedAt,
        String updatedBy
) {
}

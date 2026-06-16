package com.adem.attijari_compass.dto.admin;

public record PublicAppSettingsDto(
        boolean maintenanceMode,
        boolean chatbotEnabled,
        boolean importsEnabled,
        boolean recommendationsEnabled,
        int maxImportFileSizeMb,
        String welcomeMessage
) {
}

package com.adem.attijari_compass.service.admin;

import com.adem.attijari_compass.dto.admin.AppSettingDto;
import com.adem.attijari_compass.dto.admin.PublicAppSettingsDto;
import com.adem.attijari_compass.entity.AppSetting;
import com.adem.attijari_compass.entity.AuditStatus;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.repository.AppSettingRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppSettingService {
    private final AppSettingRepository appSettingRepository;
    private final AuditLogService auditLogService;

    @PostConstruct
    @Transactional
    public void initializeDefaults() {
        createIfMissing("maintenanceMode", "false", "BOOLEAN", "Mode maintenance global");
        createIfMissing("chatbotEnabled", "true", "BOOLEAN", "Activation visuelle du chatbot");
        createIfMissing("importsEnabled", "true", "BOOLEAN", "Activation des imports utilisateurs");
        createIfMissing("recommendationsEnabled", "true", "BOOLEAN", "Activation des recommandations");
        createIfMissing("maxImportFileSizeMb", "10", "NUMBER", "Taille maximale des fichiers importes en Mo");
        createIfMissing("welcomeMessage", "Bienvenue sur Attijari Compass", "STRING", "Message d accueil global");
    }

    public List<AppSettingDto> findAll() {
        return appSettingRepository.findAll().stream().map(this::toDto).toList();
    }

    public AppSettingDto findByKey(String key) {
        return toDto(getByKey(key));
    }

    public PublicAppSettingsDto findPublicSettings() {
        return new PublicAppSettingsDto(
                getBooleanValue("maintenanceMode", false),
                getBooleanValue("chatbotEnabled", true),
                getBooleanValue("importsEnabled", true),
                getBooleanValue("recommendationsEnabled", true),
                getIntValue("maxImportFileSizeMb", 10),
                getStringValue("welcomeMessage", "Bienvenue sur Attijari Compass")
        );
    }

    public boolean isMaintenanceMode() {
        return getBooleanValue("maintenanceMode", false);
    }

    public boolean isChatbotEnabled() {
        return getBooleanValue("chatbotEnabled", true);
    }

    public boolean isImportsEnabled() {
        return getBooleanValue("importsEnabled", true);
    }

    public boolean isRecommendationsEnabled() {
        return getBooleanValue("recommendationsEnabled", true);
    }

    public int getMaxImportFileSizeMb() {
        return getIntValue("maxImportFileSizeMb", 10);
    }

    @Transactional
    public AppSettingDto update(String key, String value, User actor) {
        AppSetting setting = getByKey(key);
        setting.setSettingValue(value);
        setting.setUpdatedBy(actor.getEmail());
        auditLogService.log(actor, "SETTING_UPDATED", "SETTINGS", AuditStatus.SUCCESS,
                "Parametre global modifie: key=" + key);
        return toDto(setting);
    }

    private void createIfMissing(String key, String value, String type, String description) {
        if (!appSettingRepository.existsBySettingKey(key)) {
            appSettingRepository.save(AppSetting.builder()
                    .settingKey(key)
                    .settingValue(value)
                    .type(type)
                    .description(description)
                    .updatedBy("SYSTEM")
                    .build());
        }
    }

    private AppSetting getByKey(String key) {
        return appSettingRepository.findBySettingKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting not found: " + key));
    }

    private boolean getBooleanValue(String key, boolean defaultValue) {
        return appSettingRepository.findBySettingKey(key)
                .map(AppSetting::getSettingValue)
                .map(String::trim)
                .map(Boolean::parseBoolean)
                .orElse(defaultValue);
    }

    private String getStringValue(String key, String defaultValue) {
        return appSettingRepository.findBySettingKey(key)
                .map(AppSetting::getSettingValue)
                .filter(value -> value != null && !value.trim().isEmpty())
                .orElse(defaultValue);
    }

    private int getIntValue(String key, int defaultValue) {
        return appSettingRepository.findBySettingKey(key)
                .map(AppSetting::getSettingValue)
                .map(String::trim)
                .map(value -> {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException ex) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    private AppSettingDto toDto(AppSetting setting) {
        return new AppSettingDto(
                setting.getId(), setting.getSettingKey(), setting.getSettingValue(), setting.getType(),
                setting.getDescription(), setting.getUpdatedAt(), setting.getUpdatedBy()
        );
    }
}

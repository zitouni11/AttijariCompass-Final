package com.adem.attijari_compass.controller;

import com.adem.attijari_compass.dto.admin.PublicAppSettingsDto;
import com.adem.attijari_compass.service.admin.AppSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/settings")
@RequiredArgsConstructor
public class AppSettingsPublicController {
    private final AppSettingService appSettingService;

    @GetMapping("/public")
    public PublicAppSettingsDto publicSettings() {
        return appSettingService.findPublicSettings();
    }
}

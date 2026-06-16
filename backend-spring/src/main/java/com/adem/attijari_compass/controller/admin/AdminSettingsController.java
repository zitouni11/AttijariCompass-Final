package com.adem.attijari_compass.controller.admin;

import com.adem.attijari_compass.dto.admin.AppSettingDto;
import com.adem.attijari_compass.dto.admin.AppSettingUpdateRequest;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.service.admin.AppSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSettingsController {
    private final AppSettingService appSettingService;

    @GetMapping
    public List<AppSettingDto> settings() {
        return appSettingService.findAll();
    }

    @GetMapping("/{key}")
    public AppSettingDto setting(@PathVariable String key) {
        return appSettingService.findByKey(key);
    }

    @PutMapping("/{key}")
    public AppSettingDto update(@PathVariable String key,
                                @Valid @RequestBody AppSettingUpdateRequest request,
                                @AuthenticationPrincipal User actor) {
        return appSettingService.update(key, request.settingValue(), actor);
    }
}

package com.adem.attijari_compass.controller;

import com.adem.attijari_compass.dto.admin.GeneralNotificationDto;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.service.admin.GeneralNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications/general")
@RequiredArgsConstructor
public class GeneralNotificationController {
    private final GeneralNotificationService generalNotificationService;

    @GetMapping
    public List<GeneralNotificationDto> visible(@AuthenticationPrincipal User user) {
        return generalNotificationService.visibleFor(user);
    }
}

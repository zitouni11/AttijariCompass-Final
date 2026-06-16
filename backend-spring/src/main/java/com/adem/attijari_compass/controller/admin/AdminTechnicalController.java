package com.adem.attijari_compass.controller.admin;

import com.adem.attijari_compass.dto.admin.TechnicalStatusDto;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.service.admin.TechnicalStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/technical")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminTechnicalController {
    private final TechnicalStatusService technicalStatusService;

    @GetMapping("/status")
    public TechnicalStatusDto status(@AuthenticationPrincipal User actor) {
        return technicalStatusService.getStatus(actor);
    }
}

package com.adem.attijari_compass.controller.admin;

import com.adem.attijari_compass.dto.admin.AdminDashboardDto;
import com.adem.attijari_compass.service.admin.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final AdminDashboardService adminDashboardService;

    @GetMapping
    public AdminDashboardDto dashboard() {
        return adminDashboardService.getDashboard();
    }
}

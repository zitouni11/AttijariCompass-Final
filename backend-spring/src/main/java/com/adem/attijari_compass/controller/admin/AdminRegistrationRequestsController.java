package com.adem.attijari_compass.controller.admin;

import com.adem.attijari_compass.dto.admin.AdminRegistrationRejectionRequest;
import com.adem.attijari_compass.dto.admin.AdminRegistrationResponseDto;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.service.admin.AdminRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/admin-requests")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminRegistrationRequestsController {
    private final AdminRegistrationService adminRegistrationService;

    @GetMapping
    public List<AdminRegistrationResponseDto> requests() {
        return adminRegistrationService.findAll();
    }

    @PatchMapping("/{id}/approve")
    public AdminRegistrationResponseDto approve(@PathVariable Long id, @AuthenticationPrincipal User actor) {
        return adminRegistrationService.approve(id, actor);
    }

    @PatchMapping("/{id}/reject")
    public AdminRegistrationResponseDto reject(@PathVariable Long id,
                                               @Valid @RequestBody AdminRegistrationRejectionRequest request,
                                               @AuthenticationPrincipal User actor) {
        return adminRegistrationService.reject(id, request.reason(), actor);
    }
}

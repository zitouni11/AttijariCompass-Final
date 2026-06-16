package com.adem.attijari_compass.controller.admin;

import com.adem.attijari_compass.dto.admin.AuditLogDto;
import com.adem.attijari_compass.service.admin.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAuditLogController {
    private final AuditLogService auditLogService;

    @GetMapping
    public List<AuditLogDto> auditLogs() {
        return auditLogService.findAll();
    }

    @GetMapping("/recent")
    public List<AuditLogDto> recent() {
        return auditLogService.recent();
    }
}

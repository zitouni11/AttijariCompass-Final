package com.adem.attijari_compass.controller.admin;

import com.adem.attijari_compass.dto.admin.AccountRestoreRequestDto;
import com.adem.attijari_compass.dto.admin.AdminActionResponse;
import com.adem.attijari_compass.dto.admin.AdminRegistrationRejectionRequest;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.service.AccountRestoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/account-restore-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccountRestoreRequestController {
    private final AccountRestoreService accountRestoreService;

    @GetMapping
    public ResponseEntity<List<AccountRestoreRequestDto>> findAll() {
        return ResponseEntity.ok(accountRestoreService.findAll());
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<AdminActionResponse> approve(@PathVariable Long id, @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(accountRestoreService.approve(id, actor));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<AdminActionResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AdminRegistrationRejectionRequest request,
            @AuthenticationPrincipal User actor
    ) {
        String reason = request != null ? request.reason() : null;
        return ResponseEntity.ok(accountRestoreService.reject(id, reason, actor));
    }
}

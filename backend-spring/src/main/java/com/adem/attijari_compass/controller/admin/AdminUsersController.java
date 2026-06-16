package com.adem.attijari_compass.controller.admin;

import com.adem.attijari_compass.dto.admin.AdminActionResponse;
import com.adem.attijari_compass.dto.admin.AdminUserDto;
import com.adem.attijari_compass.dto.admin.RoleUpdateRequest;
import com.adem.attijari_compass.dto.admin.UserDeletionRequest;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.service.admin.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUsersController {
    private final AdminUserService adminUserService;

    @GetMapping
    public List<AdminUserDto> users() {
        return adminUserService.findAll();
    }

    @GetMapping("/deleted")
    public List<AdminUserDto> deletedUsers() {
        return adminUserService.findDeleted();
    }

    @GetMapping("/{id}")
    public AdminUserDto user(@PathVariable Long id) {
        return adminUserService.findById(id);
    }

    @PatchMapping("/{id}/activate")
    public AdminUserDto activate(@PathVariable Long id, @AuthenticationPrincipal User actor) {
        return adminUserService.activate(id, actor);
    }

    @PatchMapping("/{id}/deactivate")
    public AdminUserDto deactivate(@PathVariable Long id, @AuthenticationPrincipal User actor) {
        return adminUserService.deactivate(id, actor);
    }

    @PatchMapping("/{id}/role")
    public AdminUserDto role(@PathVariable Long id,
                             @Valid @RequestBody RoleUpdateRequest request,
                             @AuthenticationPrincipal User actor) {
        return adminUserService.changeRole(id, request.role(), actor);
    }

    @PatchMapping("/{id}/delete")
    public AdminActionResponse delete(@PathVariable Long id,
                                      @Valid @RequestBody UserDeletionRequest request,
                                      @AuthenticationPrincipal User actor) {
        adminUserService.softDelete(id, request.reason(), actor);
        return new AdminActionResponse("Compte supprime avec succes.");
    }

    @PatchMapping("/{id}/restore")
    public AdminActionResponse restore(@PathVariable Long id, @AuthenticationPrincipal User actor) {
        adminUserService.restore(id, actor);
        return new AdminActionResponse("Compte restaure avec succes.");
    }
}

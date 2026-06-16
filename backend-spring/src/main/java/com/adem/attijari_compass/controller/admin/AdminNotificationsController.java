package com.adem.attijari_compass.controller.admin;

import com.adem.attijari_compass.dto.admin.GeneralNotificationDto;
import com.adem.attijari_compass.dto.admin.GeneralNotificationRequest;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.service.admin.GeneralNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminNotificationsController {
    private final GeneralNotificationService generalNotificationService;

    @GetMapping
    public List<GeneralNotificationDto> notifications() {
        return generalNotificationService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GeneralNotificationDto create(@Valid @RequestBody GeneralNotificationRequest request,
                                         @AuthenticationPrincipal User actor) {
        return generalNotificationService.create(request, actor);
    }

    @PutMapping("/{id}")
    public GeneralNotificationDto update(@PathVariable Long id,
                                         @Valid @RequestBody GeneralNotificationRequest request,
                                         @AuthenticationPrincipal User actor) {
        return generalNotificationService.update(id, request, actor);
    }

    @PatchMapping("/{id}/publish")
    public GeneralNotificationDto publish(@PathVariable Long id, @AuthenticationPrincipal User actor) {
        return generalNotificationService.publish(id, actor);
    }

    @PatchMapping("/{id}/disable")
    public GeneralNotificationDto disable(@PathVariable Long id, @AuthenticationPrincipal User actor) {
        return generalNotificationService.disable(id, actor);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal User actor) {
        generalNotificationService.delete(id, actor);
    }
}

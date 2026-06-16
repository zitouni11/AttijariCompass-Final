package com.adem.attijari_compass.controller;

import com.adem.attijari_compass.dto.notification.NotificationResponse;
import com.adem.attijari_compass.dto.notification.NotificationUnreadCountResponse;
import com.adem.attijari_compass.service.NotificationCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationCenterService notificationCenterService;

    @GetMapping("/my")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(notificationCenterService.getNotificationsForCurrentUser(userDetails.getUsername()));
    }

    @GetMapping("/my/unread-count")
    public ResponseEntity<NotificationUnreadCountResponse> getMyUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(NotificationUnreadCountResponse.builder()
                .count(notificationCenterService.getUnreadCountForCurrentUser(userDetails.getUsername()))
                .build());
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        notificationCenterService.markAsRead(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        notificationCenterService.markAllAsRead(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        notificationCenterService.deleteNotification(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}

package com.adem.attijari_compass.service.admin;

import com.adem.attijari_compass.dto.admin.GeneralNotificationDto;
import com.adem.attijari_compass.dto.admin.GeneralNotificationRequest;
import com.adem.attijari_compass.entity.*;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.repository.GeneralNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GeneralNotificationService {
    private final GeneralNotificationRepository generalNotificationRepository;
    private final AuditLogService auditLogService;

    public List<GeneralNotificationDto> findAll() {
        return generalNotificationRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @Transactional
    public GeneralNotificationDto create(GeneralNotificationRequest request, User actor) {
        GeneralNotification notification = GeneralNotification.builder()
                .title(request.title())
                .message(request.message())
                .type(request.type())
                .targetRole(request.targetRole())
                .expiresAt(request.expiresAt())
                .active(false)
                .build();
        GeneralNotification saved = generalNotificationRepository.save(notification);
        auditLogService.log(actor, "NOTIFICATION_CREATED", "NOTIFICATIONS", AuditStatus.SUCCESS,
                "Notification generale creee: notificationId=" + saved.getId());
        return toDto(saved);
    }

    @Transactional
    public GeneralNotificationDto update(Long id, GeneralNotificationRequest request, User actor) {
        GeneralNotification notification = getNotification(id);
        notification.setTitle(request.title());
        notification.setMessage(request.message());
        notification.setType(request.type());
        notification.setTargetRole(request.targetRole());
        notification.setExpiresAt(request.expiresAt());
        auditLogService.log(actor, "NOTIFICATION_UPDATED", "NOTIFICATIONS", AuditStatus.SUCCESS,
                "Notification generale modifiee: notificationId=" + id);
        return toDto(notification);
    }

    @Transactional
    public GeneralNotificationDto publish(Long id, User actor) {
        GeneralNotification notification = getNotification(id);
        notification.setActive(true);
        notification.setPublishedAt(LocalDateTime.now());
        auditLogService.log(actor, "NOTIFICATION_PUBLISHED", "NOTIFICATIONS", AuditStatus.SUCCESS,
                "Notification generale publiee: notificationId=" + id);
        return toDto(notification);
    }

    @Transactional
    public GeneralNotificationDto disable(Long id, User actor) {
        GeneralNotification notification = getNotification(id);
        notification.setActive(false);
        auditLogService.log(actor, "NOTIFICATION_DISABLED", "NOTIFICATIONS", AuditStatus.SUCCESS,
                "Notification generale desactivee: notificationId=" + id);
        return toDto(notification);
    }

    @Transactional
    public void delete(Long id, User actor) {
        if (!generalNotificationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Notification introuvable.");
        }
        generalNotificationRepository.deleteById(id);
        auditLogService.log(actor, "NOTIFICATION_DELETED", "NOTIFICATIONS", AuditStatus.SUCCESS,
                "Notification generale supprimee: notificationId=" + id);
    }

    public List<GeneralNotificationDto> visibleFor(User user) {
        NotificationTargetRole role = user.getRole() == Role.ADMIN ? NotificationTargetRole.ADMIN : NotificationTargetRole.USER;
        return generalNotificationRepository
                .findVisibleForRole(List.of(NotificationTargetRole.ALL, role), LocalDateTime.now())
                .stream().map(this::toDto).toList();
    }

    private GeneralNotification getNotification(Long id) {
        return generalNotificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable."));
    }

    private GeneralNotificationDto toDto(GeneralNotification notification) {
        return new GeneralNotificationDto(
                notification.getId(), notification.getTitle(), notification.getMessage(),
                notification.getType(), notification.getTargetRole(), Boolean.TRUE.equals(notification.getActive()),
                notification.getCreatedAt(), notification.getPublishedAt(), notification.getExpiresAt()
        );
    }
}

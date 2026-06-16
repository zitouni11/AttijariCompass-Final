package com.adem.attijari_compass.service.admin;

import com.adem.attijari_compass.dto.admin.AuditLogDto;
import com.adem.attijari_compass.entity.AuditLog;
import com.adem.attijari_compass.entity.AuditStatus;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public void log(User actor, String action, String module, AuditStatus status, String message) {
        auditLogRepository.save(AuditLog.builder()
                .actorId(actor != null ? actor.getId() : null)
                .actorEmail(actor != null ? actor.getEmail() : "SYSTEM")
                .actorRole(actor != null && actor.getRole() != null ? actor.getRole().name() : "SYSTEM")
                .action(action)
                .module(module)
                .status(status)
                .message(message)
                .build());
    }

    public List<AuditLogDto> findAll() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    public List<AuditLogDto> recent() {
        return auditLogRepository.findByOrderByCreatedAtDesc(PageRequest.of(0, 25)).stream().map(this::toDto).toList();
    }

    private AuditLogDto toDto(AuditLog log) {
        return new AuditLogDto(
                log.getId(),
                log.getActorId(),
                log.getActorEmail(),
                log.getActorRole(),
                log.getAction(),
                log.getModule(),
                log.getStatus(),
                log.getMessage(),
                log.getCreatedAt()
        );
    }
}

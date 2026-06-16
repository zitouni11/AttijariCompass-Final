package com.adem.attijari_compass.service.admin;

import com.adem.attijari_compass.dto.admin.AdminUserDto;
import com.adem.attijari_compass.entity.AuditStatus;
import com.adem.attijari_compass.entity.Role;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.repository.RefreshTokenRepository;
import com.adem.attijari_compass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogService auditLogService;

    public List<AdminUserDto> findAll() {
        return userRepository.findAllByDeletedFalse().stream().map(this::toDto).toList();
    }

    public List<AdminUserDto> findDeleted() {
        return userRepository.findAllByDeletedTrue().stream().map(this::toDto).toList();
    }

    public AdminUserDto findById(Long id) {
        return toDto(getUser(id));
    }

    @Transactional
    public AdminUserDto activate(Long id, User actor) {
        User user = getUser(id);
        ensureNotDeleted(user);
        user.setActive(true);
        auditLogService.log(actor, "USER_ACTIVATED", "USERS", AuditStatus.SUCCESS,
                "Compte utilisateur active: userId=" + id);
        return toDto(user);
    }

    @Transactional
    public AdminUserDto deactivate(Long id, User actor) {
        User user = getUser(id);
        ensureNotDeleted(user);
        user.setActive(false);
        auditLogService.log(actor, "USER_DEACTIVATED", "USERS", AuditStatus.SUCCESS,
                "Compte utilisateur desactive: userId=" + id);
        return toDto(user);
    }

    @Transactional
    public AdminUserDto changeRole(Long id, Role role, User actor) {
        User user = getUser(id);
        ensureNotDeleted(user);
        user.setRole(role);
        auditLogService.log(actor, "ROLE_CHANGED", "USERS", AuditStatus.SUCCESS,
                "Role modifie: userId=" + id + ", role=" + role.name());
        return toDto(user);
    }

    @Transactional
    public AdminUserDto softDelete(Long id, String reason, User actor) {
        User user = getUser(id);
        if (Boolean.TRUE.equals(user.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Compte deja supprime.");
        }
        if (actor != null && actor.getId() != null && actor.getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Vous ne pouvez pas supprimer votre propre compte administrateur.");
        }
        if (user.getRole() == Role.ADMIN && userRepository.countByRoleAndActiveTrueAndDeletedFalse(Role.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Impossible de supprimer le dernier administrateur actif.");
        }

        user.setActive(false);
        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        user.setDeletedBy(actor != null ? actor.getId() : null);
        user.setDeletionReason(StringUtils.hasText(reason) ? reason.trim() : "Suppression par administrateur");
        invalidateRefreshTokens(user);
        logAuditSafely(actor, "USER_DELETED", "Compte supprime logiquement : " + user.getEmail());
        return toDto(user);
    }

    @Transactional
    public AdminUserDto restore(Long id, User actor) {
        User user = getUser(id);
        if (!Boolean.TRUE.equals(user.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce compte n est pas supprime.");
        }

        user.setDeleted(false);
        user.setActive(true);
        user.setDeletedAt(null);
        user.setDeletedBy(null);
        user.setDeletionReason(null);
        logAuditSafely(actor, "USER_RESTORED", "Compte restaure : " + user.getEmail());
        return toDto(user);
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable."));
    }

    private void ensureNotDeleted(User user) {
        if (Boolean.TRUE.equals(user.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Compte supprime.");
        }
    }

    private void invalidateRefreshTokens(User user) {
        try {
            refreshTokenRepository.deleteByUserId(user.getId());
        } catch (RuntimeException ex) {
            log.warn("Unable to invalidate refresh tokens for deleted userId={}: {}", user.getId(), ex.getMessage());
        }
    }

    private void logAuditSafely(User actor, String action, String message) {
        try {
            auditLogService.log(actor, action, "USERS", AuditStatus.SUCCESS, message);
        } catch (RuntimeException ex) {
            log.warn("Unable to write audit log for action {}: {}", action, ex.getMessage());
        }
    }

    private AdminUserDto toDto(User user) {
        return new AdminUserDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                Boolean.TRUE.equals(user.getActive()),
                Boolean.TRUE.equals(user.getDeleted()),
                user.getDeletedAt(),
                user.getDeletionReason(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }
}

package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.admin.AdminActionResponse;
import com.adem.attijari_compass.dto.auth.AccountRestoreVerifyRequest;
import com.adem.attijari_compass.dto.auth.AuthMessageResponse;
import com.adem.attijari_compass.entity.AccountRestoreRequest;
import com.adem.attijari_compass.entity.AccountRestoreStatus;
import com.adem.attijari_compass.entity.AuditStatus;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.exception.EmailDeliveryException;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.repository.AccountRestoreRequestRepository;
import com.adem.attijari_compass.repository.UserRepository;
import com.adem.attijari_compass.service.admin.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountRestoreService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;

    private final AccountRestoreRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    @Transactional
    public AuthMessageResponse request(com.adem.attijari_compass.dto.auth.AccountRestoreRequestDto request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return new AuthMessageResponse("Si un compte supprime existe avec cette adresse, une demande pourra etre traitee.");
        }
        if (!Boolean.TRUE.equals(user.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce compte existe deja. Veuillez vous connecter.");
        }

        AccountRestoreRequest latest = requestRepository.findTopByEmailOrderByRequestedAtDesc(email).orElse(null);
        if (latest != null && latest.getStatus() == AccountRestoreStatus.PENDING_ADMIN_APPROVAL) {
            return new AuthMessageResponse("Votre demande de restauration est deja en attente de validation par un administrateur.");
        }

        AccountRestoreRequest entity = latest != null && latest.getStatus() == AccountRestoreStatus.PENDING_EMAIL_VERIFICATION
                ? latest
                : new AccountRestoreRequest();

        String code = generateCode();
        String fullName = StringUtils.hasText(user.getFullName()) ? user.getFullName().trim() : user.getEmail();
        entity.setEmail(email);
        entity.setFullName(fullName);
        entity.setVerificationCodeHash(passwordEncoder.encode(code));
        entity.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(10));
        entity.setVerificationAttempts(0);
        entity.setEmailVerified(false);
        entity.setStatus(AccountRestoreStatus.PENDING_EMAIL_VERIFICATION);
        entity.setRequestedAt(LocalDateTime.now());
        entity.setVerifiedAt(null);
        entity.setApprovedAt(null);
        entity.setApprovedBy(null);
        entity.setRejectedAt(null);
        entity.setRejectedBy(null);
        entity.setRejectionReason(null);

        requestRepository.save(entity);
        try {
            emailService.sendAccountRestoreVerificationCode(email, fullName, code);
        } catch (EmailDeliveryException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Service e-mail temporairement indisponible. Veuillez reessayer plus tard.",
                    ex
            );
        }
        return new AuthMessageResponse("Un code de verification a ete envoye a votre adresse e-mail.");
    }

    @Transactional
    public AuthMessageResponse verify(AccountRestoreVerifyRequest request) {
        String email = normalizeEmail(request.email());
        AccountRestoreRequest entity = requestRepository.findTopByEmailOrderByRequestedAtDesc(email)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de restauration introuvable."));

        if (entity.getStatus() != AccountRestoreStatus.PENDING_EMAIL_VERIFICATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette demande ne peut plus etre verifiee.");
        }
        if (entity.getVerificationCodeExpiresAt() == null
                || entity.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            entity.setStatus(AccountRestoreStatus.EXPIRED);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code expire. Veuillez demander un nouveau code.");
        }
        if (safeAttempts(entity) >= MAX_VERIFICATION_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nombre maximum de tentatives atteint. Veuillez demander un nouveau code.");
        }
        if (!StringUtils.hasText(entity.getVerificationCodeHash())
                || !passwordEncoder.matches(request.code(), entity.getVerificationCodeHash())) {
            entity.setVerificationAttempts(safeAttempts(entity) + 1);
            if (safeAttempts(entity) >= MAX_VERIFICATION_ATTEMPTS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Nombre maximum de tentatives atteint. Veuillez demander un nouveau code.");
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code de verification invalide.");
        }

        entity.setEmailVerified(true);
        entity.setStatus(AccountRestoreStatus.PENDING_ADMIN_APPROVAL);
        entity.setVerifiedAt(LocalDateTime.now());
        entity.setVerificationCodeHash(null);
        entity.setVerificationCodeExpiresAt(null);
        entity.setVerificationAttempts(0);
        return new AuthMessageResponse(
                "Votre adresse e-mail est verifiee. Votre demande de restauration est en attente de validation par un administrateur.");
    }

    public List<com.adem.attijari_compass.dto.admin.AccountRestoreRequestDto> findAll() {
        return requestRepository.findAllByOrderByRequestedAtDesc().stream().map(this::toDto).toList();
    }

    @Transactional
    public AdminActionResponse approve(Long id, User actor) {
        AccountRestoreRequest request = getRequest(id);
        if (request.getStatus() != AccountRestoreStatus.PENDING_ADMIN_APPROVAL || !Boolean.TRUE.equals(request.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette demande ne peut pas etre approuvee.");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable."));
        if (!Boolean.TRUE.equals(user.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce compte n est pas supprime.");
        }

        user.setDeleted(false);
        user.setActive(true);
        user.setDeletedAt(null);
        user.setDeletedBy(null);
        user.setDeletionReason(null);

        request.setStatus(AccountRestoreStatus.APPROVED);
        request.setApprovedAt(LocalDateTime.now());
        request.setApprovedBy(actor != null ? actor.getId() : null);
        emailService.sendAccountRestoreApprovedEmail(request.getEmail(), request.getFullName());
        logAuditSafely(actor, "ACCOUNT_RESTORE_APPROVED", "Demande de restauration approuvee: requestId=" + request.getId());
        return new AdminActionResponse("Compte restaure avec succes.");
    }

    @Transactional
    public AdminActionResponse reject(Long id, String reason, User actor) {
        AccountRestoreRequest request = getRequest(id);
        if (request.getStatus() == AccountRestoreStatus.APPROVED || request.getStatus() == AccountRestoreStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette demande est deja traitee.");
        }

        String cleanReason = StringUtils.hasText(reason) ? reason.trim() : "Non precise";
        request.setStatus(AccountRestoreStatus.REJECTED);
        request.setRejectedAt(LocalDateTime.now());
        request.setRejectedBy(actor != null ? actor.getId() : null);
        request.setRejectionReason(cleanReason);
        emailService.sendAccountRestoreRejectedEmail(request.getEmail(), request.getFullName(), cleanReason);
        logAuditSafely(actor, "ACCOUNT_RESTORE_REJECTED", "Demande de restauration refusee: requestId=" + request.getId());
        return new AdminActionResponse("Demande de restauration refusee.");
    }

    private AccountRestoreRequest getRequest(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de restauration introuvable."));
    }

    private com.adem.attijari_compass.dto.admin.AccountRestoreRequestDto toDto(AccountRestoreRequest request) {
        return new com.adem.attijari_compass.dto.admin.AccountRestoreRequestDto(
                request.getId(),
                request.getEmail(),
                request.getFullName(),
                Boolean.TRUE.equals(request.getEmailVerified()),
                request.getStatus().name(),
                request.getRequestedAt(),
                request.getVerifiedAt(),
                request.getApprovedAt(),
                request.getApprovedBy(),
                request.getRejectedAt(),
                request.getRejectedBy(),
                request.getRejectionReason()
        );
    }

    private void logAuditSafely(User actor, String action, String message) {
        try {
            auditLogService.log(actor, action, "USERS", AuditStatus.SUCCESS, message);
        } catch (RuntimeException ex) {
            log.warn("Unable to write audit log for action {}: {}", action, ex.getMessage());
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String generateCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private int safeAttempts(AccountRestoreRequest request) {
        return request.getVerificationAttempts() != null ? request.getVerificationAttempts() : 0;
    }
}

package com.adem.attijari_compass.service.admin;

import com.adem.attijari_compass.dto.admin.AdminRegistrationResponseDto;
import com.adem.attijari_compass.dto.auth.AdminRegistrationRequestDto;
import com.adem.attijari_compass.dto.auth.AdminRegistrationVerifyRequest;
import com.adem.attijari_compass.entity.AdminRegistrationRequest;
import com.adem.attijari_compass.entity.AdminRegistrationStatus;
import com.adem.attijari_compass.entity.AuditStatus;
import com.adem.attijari_compass.entity.Role;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.exception.EmailAlreadyExistsException;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.repository.AdminRegistrationRequestRepository;
import com.adem.attijari_compass.repository.UserRepository;
import com.adem.attijari_compass.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminRegistrationService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Set<AdminRegistrationStatus> ACTIVE_REQUEST_STATUSES = Set.of(
            AdminRegistrationStatus.EMAIL_VERIFICATION_PENDING,
            AdminRegistrationStatus.PENDING_APPROVAL
    );

    private final AdminRegistrationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    @Transactional
    public AdminRegistrationResponseDto request(AdminRegistrationRequestDto request) {
        String email = normalizeEmail(request.email());
        User existingUser = userRepository.findByEmail(email).orElse(null);
        if (existingUser != null) {
            if (Boolean.TRUE.equals(existingUser.getDeleted())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Un compte supprime existe deja avec cette adresse e-mail. Veuillez demander a un administrateur de restaurer ce compte.");
            }
            throw new EmailAlreadyExistsException("Email already in use: " + email);
        }
        if (requestRepository.existsByEmailAndStatusIn(email, ACTIVE_REQUEST_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une demande administrateur est deja en cours pour cet e-mail.");
        }

        String code = generateCode();
        AdminRegistrationRequest entity = AdminRegistrationRequest.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .verificationCodeHash(passwordEncoder.encode(code))
                .verificationExpiresAt(LocalDateTime.now().plusMinutes(10))
                .status(AdminRegistrationStatus.EMAIL_VERIFICATION_PENDING)
                .build();

        AdminRegistrationRequest saved = requestRepository.save(entity);
        emailService.sendAdminVerificationCode(email, saved.getFullName(), code);
        auditLogService.log(null, "ADMIN_REGISTRATION_REQUESTED", "AUTH", AuditStatus.SUCCESS,
                "Demande de compte administrateur creee: requestId=" + saved.getId());
        return toDto(saved);
    }

    @Transactional
    public AdminRegistrationResponseDto verify(AdminRegistrationVerifyRequest request) {
        String email = normalizeEmail(request.email());
        AdminRegistrationRequest entity = requestRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new ResourceNotFoundException("Admin registration request not found"));

        if (entity.getStatus() != AdminRegistrationStatus.EMAIL_VERIFICATION_PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette demande ne peut plus etre verifiee.");
        }
        if (entity.getVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            entity.setStatus(AdminRegistrationStatus.EXPIRED);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le code de verification a expire.");
        }
        if (!passwordEncoder.matches(request.code(), entity.getVerificationCodeHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code de verification invalide.");
        }

        entity.setStatus(AdminRegistrationStatus.PENDING_APPROVAL);
        entity.setVerifiedAt(LocalDateTime.now());
        auditLogService.log(null, "ADMIN_REGISTRATION_VERIFIED", "AUTH", AuditStatus.SUCCESS,
                "Demande de compte administrateur verifiee: requestId=" + entity.getId());
        return toDto(entity);
    }

    public List<AdminRegistrationResponseDto> findAll() {
        return requestRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @Transactional
    public AdminRegistrationResponseDto approve(Long id, User actor) {
        AdminRegistrationRequest request = getRequest(id);
        if (request.getStatus() != AdminRegistrationStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette demande ne peut pas etre approuvee.");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already in use: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(request.getPasswordHash())
                .fullName(request.getFullName())
                .role(Role.ADMIN)
                .active(true)
                .build();
        userRepository.save(user);

        request.setStatus(AdminRegistrationStatus.APPROVED);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedByEmail(actor != null ? actor.getEmail() : "SYSTEM");
        emailService.sendAdminApprovalEmail(request.getEmail(), request.getFullName());
        auditLogService.log(actor, "ADMIN_REQUEST_APPROVED", "AUTH", AuditStatus.SUCCESS,
                "Demande administrateur approuvee: requestId=" + request.getId());
        return toDto(request);
    }

    @Transactional
    public AdminRegistrationResponseDto reject(Long id, String reason, User actor) {
        AdminRegistrationRequest request = getRequest(id);
        if (request.getStatus() == AdminRegistrationStatus.APPROVED || request.getStatus() == AdminRegistrationStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette demande est deja traitee.");
        }

        String cleanReason = StringUtils.hasText(reason) ? reason.trim() : "Non precise";
        request.setStatus(AdminRegistrationStatus.REJECTED);
        request.setRejectionReason(cleanReason);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedByEmail(actor != null ? actor.getEmail() : "SYSTEM");
        emailService.sendAdminRejectionEmail(request.getEmail(), request.getFullName(), cleanReason);
        auditLogService.log(actor, "ADMIN_REQUEST_REJECTED", "AUTH", AuditStatus.SUCCESS,
                "Demande administrateur refusee: requestId=" + request.getId());
        return toDto(request);
    }

    private AdminRegistrationRequest getRequest(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin registration request not found with id: " + id));
    }

    private String generateCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private AdminRegistrationResponseDto toDto(AdminRegistrationRequest request) {
        return new AdminRegistrationResponseDto(
                request.getId(),
                request.getFullName(),
                request.getEmail(),
                request.getStatus().name(),
                request.getCreatedAt(),
                request.getVerifiedAt(),
                request.getReviewedAt(),
                request.getReviewedByEmail(),
                request.getRejectionReason()
        );
    }
}

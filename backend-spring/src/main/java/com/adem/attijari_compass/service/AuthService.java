package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.auth.AuthResponse;
import com.adem.attijari_compass.dto.auth.AuthMessageResponse;
import com.adem.attijari_compass.dto.auth.LoginRequest;
import com.adem.attijari_compass.dto.auth.RefreshTokenRequest;
import com.adem.attijari_compass.dto.auth.RegisterRequest;
import com.adem.attijari_compass.dto.auth.ResendVerificationCodeRequest;
import com.adem.attijari_compass.dto.auth.VerifyEmailRequest;
import com.adem.attijari_compass.entity.RefreshToken;
import com.adem.attijari_compass.entity.Role;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.entity.AuditStatus;
import com.adem.attijari_compass.exception.EmailAlreadyExistsException;
import com.adem.attijari_compass.exception.InvalidRefreshTokenException;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.repository.RefreshTokenRepository;
import com.adem.attijari_compass.repository.UserRepository;
import com.adem.attijari_compass.security.JwtService;
import com.adem.attijari_compass.service.admin.AuditLogService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EntityManager entityManager;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    @Transactional
    public AuthMessageResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (request.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La creation d un compte administrateur doit passer par le workflow de verification.");
        }
        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            if (Boolean.TRUE.equals(existing.getDeleted())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Un compte supprime existe deja avec cette adresse e-mail. Veuillez contacter l administrateur pour restaurer le compte.");
            }
            throw new EmailAlreadyExistsException("Un compte existe deja avec cet e-mail.");
        }
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .active(false)
                .emailVerified(false)
                .verificationAttempts(0)
                .build();
        String code = generateVerificationCode();
        user.setVerificationCodeHash(passwordEncoder.encode(code));
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);
        emailService.sendUserVerificationCode(user.getEmail(), user.getFullName(), code);
        auditLogService.log(null, "USER_REGISTRATION_REQUESTED", "AUTH", AuditStatus.SUCCESS,
                "Inscription utilisateur en attente de verification: userId=" + user.getId());
        return new AuthMessageResponse("Un code de verification a ete envoye a votre adresse e-mail.");
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User existingUser = userRepository.findByEmail(email).orElse(null);
        if (existingUser != null && Boolean.TRUE.equals(existingUser.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ce compte a ete supprime. Veuillez contacter l administrateur.");
        }
        if (existingUser != null && Boolean.FALSE.equals(existingUser.getActive())) {
            if (existingUser.getRole() == Role.USER && Boolean.FALSE.equals(existingUser.getEmailVerified())) {
                auditLogService.log(null, "USER_LOGIN_BLOCKED_EMAIL_NOT_VERIFIED", "AUTH", AuditStatus.FAILED,
                        "Connexion utilisateur bloquee avant verification e-mail: userId=" + existingUser.getId());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Veuillez verifier votre adresse e-mail avant de vous connecter.");
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Votre compte est desactive.");
        }
        if (existingUser != null
                && existingUser.getRole() == Role.USER
                && Boolean.FALSE.equals(existingUser.getEmailVerified())) {
            auditLogService.log(null, "USER_LOGIN_BLOCKED_EMAIL_NOT_VERIFIED", "AUTH", AuditStatus.FAILED,
                    "Connexion utilisateur bloquee avant verification e-mail: userId=" + existingUser.getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Veuillez verifier votre adresse e-mail avant de vous connecter.");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        String token = jwtService.generateToken(user);
        String refreshToken = createRefreshToken(user);
        return AuthResponse.builder()
                .accessToken(token).token(token).refreshToken(refreshToken)
                .email(user.getEmail()).role(user.getRole().name()).fullName(user.getFullName())
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tokenValue = request != null ? request.getRefreshToken() : null;
        if (!StringUtils.hasText(tokenValue)) {
            log.warn("Refresh token rejected: missing token in request");
            throw new InvalidRefreshTokenException("Refresh token invalide ou manquant.");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByTokenForUpdate(tokenValue.trim())
                .orElseThrow(() -> {
                    log.warn("Refresh token rejected: token not found");
                    return new InvalidRefreshTokenException("Refresh token invalide ou expire.");
                });

        if (refreshToken.isExpired()) {
            log.warn("Refresh token rejected: token expired for userId={}", refreshToken.getUser().getId());
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidRefreshTokenException("Refresh token invalide ou expire.");
        }

        User user = refreshToken.getUser();
        if (Boolean.TRUE.equals(user.getDeleted())) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidRefreshTokenException("Ce compte a ete supprime. Veuillez contacter l administrateur.");
        }
        if (Boolean.FALSE.equals(user.getActive())) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidRefreshTokenException("Votre compte est desactive.");
        }
        refreshTokenRepository.delete(refreshToken);
        entityManager.flush();

        String newToken = jwtService.generateToken(user);
        String newRefreshToken = createRefreshToken(user);
        log.info("Refresh token accepted for userId={}", user.getId());

        return AuthResponse.builder()
                .accessToken(newToken).token(newToken).refreshToken(newRefreshToken)
                .email(user.getEmail()).role(user.getRole().name()).fullName(user.getFullName())
                .build();
    }

    @Transactional
    public AuthMessageResponse verifyEmail(VerifyEmailRequest request) {
        User user = getUserForEmailVerification(request.email());

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce compte est deja verifie.");
        }
        if (user.getVerificationCodeExpiresAt() == null || user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code expire. Veuillez demander un nouveau code.");
        }
        if (safeAttempts(user) >= MAX_VERIFICATION_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nombre maximum de tentatives atteint. Veuillez demander un nouveau code.");
        }
        if (!StringUtils.hasText(user.getVerificationCodeHash())
                || !passwordEncoder.matches(request.code(), user.getVerificationCodeHash())) {
            user.setVerificationAttempts(safeAttempts(user) + 1);
            if (safeAttempts(user) >= MAX_VERIFICATION_ATTEMPTS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Nombre maximum de tentatives atteint. Veuillez demander un nouveau code.");
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code de verification invalide.");
        }

        user.setEmailVerified(true);
        user.setActive(true);
        user.setVerificationCodeHash(null);
        user.setVerificationCodeExpiresAt(null);
        user.setVerificationAttempts(0);
        auditLogService.log(null, "USER_EMAIL_VERIFIED", "AUTH", AuditStatus.SUCCESS,
                "E-mail utilisateur verifie: userId=" + user.getId());
        return new AuthMessageResponse("Votre compte a ete verifie avec succes. Vous pouvez maintenant vous connecter.");
    }

    @Transactional
    public AuthMessageResponse resendVerificationCode(ResendVerificationCodeRequest request) {
        User user = getUserForEmailVerification(request.email());

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce compte est deja verifie.");
        }

        String code = generateVerificationCode();
        user.setVerificationCodeHash(passwordEncoder.encode(code));
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(10));
        user.setVerificationAttempts(0);
        emailService.sendUserVerificationCode(user.getEmail(), user.getFullName(), code);
        auditLogService.log(null, "USER_VERIFICATION_CODE_RESENT", "AUTH", AuditStatus.SUCCESS,
                "Nouveau code de verification utilisateur envoye: userId=" + user.getId());
        return new AuthMessageResponse("Un nouveau code de verification a ete envoye.");
    }

    private String createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user);
        entityManager.flush(); // ← Force le delete avant l'insert
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        return refreshTokenRepository.save(refreshToken).getToken();
    }

    private User getUserForEmailVerification(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette verification est reservee aux comptes utilisateur.");
        }
        return user;
    }

    private String generateVerificationCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private int safeAttempts(User user) {
        return user.getVerificationAttempts() != null ? user.getVerificationAttempts() : 0;
    }
}

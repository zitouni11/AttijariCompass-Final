package com.adem.attijari_compass.controller;

import com.adem.attijari_compass.dto.auth.AuthResponse;
import com.adem.attijari_compass.dto.auth.AuthMessageResponse;
import com.adem.attijari_compass.dto.auth.LoginRequest;
import com.adem.attijari_compass.dto.auth.RefreshTokenRequest;
import com.adem.attijari_compass.dto.auth.RegisterRequest;
import com.adem.attijari_compass.dto.auth.ResendVerificationCodeRequest;
import com.adem.attijari_compass.dto.auth.VerifyEmailRequest;
import com.adem.attijari_compass.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthMessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthMessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request));
    }

    @PostMapping("/resend-verification-code")
    public ResponseEntity<AuthMessageResponse> resendVerificationCode(@Valid @RequestBody ResendVerificationCodeRequest request) {
        return ResponseEntity.ok(authService.resendVerificationCode(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody(required = false) RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }
}

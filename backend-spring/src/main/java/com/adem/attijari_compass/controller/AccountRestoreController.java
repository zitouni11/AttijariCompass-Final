package com.adem.attijari_compass.controller;

import com.adem.attijari_compass.dto.auth.AccountRestoreRequestDto;
import com.adem.attijari_compass.dto.auth.AccountRestoreVerifyRequest;
import com.adem.attijari_compass.dto.auth.AuthMessageResponse;
import com.adem.attijari_compass.service.AccountRestoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/account-restore")
@RequiredArgsConstructor
public class AccountRestoreController {
    private final AccountRestoreService accountRestoreService;

    @PostMapping("/request")
    public ResponseEntity<AuthMessageResponse> request(@Valid @RequestBody AccountRestoreRequestDto request) {
        return ResponseEntity.ok(accountRestoreService.request(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<AuthMessageResponse> verify(@Valid @RequestBody AccountRestoreVerifyRequest request) {
        return ResponseEntity.ok(accountRestoreService.verify(request));
    }
}

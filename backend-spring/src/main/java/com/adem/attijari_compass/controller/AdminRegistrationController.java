package com.adem.attijari_compass.controller;

import com.adem.attijari_compass.dto.admin.AdminRegistrationResponseDto;
import com.adem.attijari_compass.dto.auth.AdminRegistrationRequestDto;
import com.adem.attijari_compass.dto.auth.AdminRegistrationVerifyRequest;
import com.adem.attijari_compass.service.admin.AdminRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/admin-registration")
@RequiredArgsConstructor
public class AdminRegistrationController {
    private final AdminRegistrationService adminRegistrationService;

    @PostMapping("/request")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminRegistrationResponseDto request(@Valid @RequestBody AdminRegistrationRequestDto request) {
        return adminRegistrationService.request(request);
    }

    @PostMapping("/verify")
    public AdminRegistrationResponseDto verify(@Valid @RequestBody AdminRegistrationVerifyRequest request) {
        return adminRegistrationService.verify(request);
    }
}

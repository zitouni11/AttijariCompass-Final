package com.adem.attijari_compass.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AccountRestoreRequestDto(@NotBlank @Email String email) {
}

package com.adem.attijari_compass.dto.admin;

import com.adem.attijari_compass.entity.Role;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(@NotNull Role role) {
}

package com.adem.attijari_compass.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AppSettingUpdateRequest(@NotBlank @Size(max = 3000) String settingValue) {
}

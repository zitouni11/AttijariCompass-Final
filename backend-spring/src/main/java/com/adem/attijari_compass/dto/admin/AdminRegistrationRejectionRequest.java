package com.adem.attijari_compass.dto.admin;

import jakarta.validation.constraints.Size;

public record AdminRegistrationRejectionRequest(
        @Size(max = 1000) String reason
) {
}

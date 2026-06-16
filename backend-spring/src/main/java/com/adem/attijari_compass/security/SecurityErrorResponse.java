package com.adem.attijari_compass.security;

import java.time.LocalDateTime;

public record SecurityErrorResponse(
        int status,
        String message,
        LocalDateTime timestamp,
        String details
) {
}

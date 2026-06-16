package com.adem.attijari_compass.dto.admin;

import com.adem.attijari_compass.entity.SupportTicketCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SupportTicketCreateRequest(
        @NotBlank @Size(max = 160) String subject,
        @NotNull SupportTicketCategory category,
        @NotBlank @Size(max = 3000) String message
) {
}

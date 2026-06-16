package com.adem.attijari_compass.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportTicketReplyRequest(@NotBlank @Size(max = 3000) String adminReply) {
}

package com.adem.attijari_compass.dto.admin;

import com.adem.attijari_compass.entity.SupportTicketStatus;
import jakarta.validation.constraints.NotNull;

public record SupportTicketStatusRequest(@NotNull SupportTicketStatus status) {
}

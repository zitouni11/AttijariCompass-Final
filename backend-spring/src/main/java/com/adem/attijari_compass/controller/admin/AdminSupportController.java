package com.adem.attijari_compass.controller.admin;

import com.adem.attijari_compass.dto.admin.SupportTicketDto;
import com.adem.attijari_compass.dto.admin.SupportTicketReplyRequest;
import com.adem.attijari_compass.dto.admin.SupportTicketStatusRequest;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.service.admin.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/support/tickets")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSupportController {
    private final SupportTicketService supportTicketService;

    @GetMapping
    public List<SupportTicketDto> tickets() {
        return supportTicketService.findAll();
    }

    @GetMapping("/{id}")
    public SupportTicketDto ticket(@PathVariable Long id) {
        return supportTicketService.findById(id);
    }

    @PatchMapping("/{id}/status")
    public SupportTicketDto status(@PathVariable Long id,
                                   @Valid @RequestBody SupportTicketStatusRequest request,
                                   @AuthenticationPrincipal User actor) {
        return supportTicketService.updateStatus(id, request.status(), actor);
    }

    @PostMapping("/{id}/reply")
    public SupportTicketDto reply(@PathVariable Long id,
                                  @Valid @RequestBody SupportTicketReplyRequest request,
                                  @AuthenticationPrincipal User actor) {
        return supportTicketService.reply(id, request.adminReply(), actor);
    }
}

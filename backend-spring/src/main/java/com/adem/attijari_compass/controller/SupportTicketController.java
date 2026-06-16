package com.adem.attijari_compass.controller;

import com.adem.attijari_compass.dto.admin.SupportTicketCreateRequest;
import com.adem.attijari_compass.dto.admin.SupportTicketDto;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.service.admin.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support/tickets")
@RequiredArgsConstructor
public class SupportTicketController {
    private final SupportTicketService supportTicketService;

    @PostMapping
    public SupportTicketDto create(@Valid @RequestBody SupportTicketCreateRequest request,
                                   @AuthenticationPrincipal User user) {
        return supportTicketService.create(request, user);
    }

    @GetMapping("/my")
    public List<SupportTicketDto> mine(@AuthenticationPrincipal User user) {
        return supportTicketService.mine(user);
    }
}

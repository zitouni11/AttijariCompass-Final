package com.adem.attijari_compass.chat.controller;

import com.adem.attijari_compass.chat.dto.ChatRequestDto;
import com.adem.attijari_compass.chat.dto.ChatResponseDto;
import com.adem.attijari_compass.chat.service.ChatService;
import com.adem.attijari_compass.chat.service.GroqService;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.exception.AuthenticationRequiredException;
import com.adem.attijari_compass.repository.UserRepository;
import com.adem.attijari_compass.service.admin.AppSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final AppSettingService appSettingService;

    @PostMapping
    public ResponseEntity<ChatResponseDto> chat(@Valid @RequestBody ChatRequestDto request,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationRequiredException("Authentication is required");
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationRequiredException("Authenticated user could not be resolved"));

        if (appSettingService.isMaintenanceMode()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "L application est temporairement en maintenance."
            );
        }

        if (!appSettingService.isChatbotEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Le chatbot est temporairement desactive par l administrateur."
            );
        }

        try {
            return ResponseEntity.ok(chatService.chat(user.getId(), request.getMessage()));
        } catch (GroqService.GroqCallException ex) {
            HttpStatus status = ex.getStatusCode() == 429
                    ? HttpStatus.TOO_MANY_REQUESTS
                    : HttpStatus.SERVICE_UNAVAILABLE;
            String message = ex.getStatusCode() == 429
                    ? "Le service IA est temporairement sollicite. Reessayez dans quelques instants."
                    : "Le service IA est temporairement indisponible.";
            log.warn("Chat request failed for userId={}: {}", user.getId(), ex.getMessage());
            throw new ResponseStatusException(status, message, ex);
        } catch (Exception ex) {
            log.error("Unexpected chat failure for userId={}: {}", user.getId(), ex.getMessage(), ex);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Le service IA est temporairement indisponible.",
                    ex
            );
        }
    }
}

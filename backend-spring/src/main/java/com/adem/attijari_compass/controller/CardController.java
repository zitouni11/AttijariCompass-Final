package com.adem.attijari_compass.controller;

import com.adem.attijari_compass.dto.card.CardCatalogDto;
import com.adem.attijari_compass.dto.card.CardActionResponse;
import com.adem.attijari_compass.dto.card.CardDetailsDto;
import com.adem.attijari_compass.dto.card.CardSummaryDto;
import com.adem.attijari_compass.dto.card.CardSyncResponse;
import com.adem.attijari_compass.dto.card.CardTransactionDto;
import com.adem.attijari_compass.dto.card.CardTransactionResponse;
import com.adem.attijari_compass.dto.card.ConnectTestCardRequest;
import com.adem.attijari_compass.dto.card.GenerateTestCardRequest;
import com.adem.attijari_compass.dto.card.GenerateTestCardResponse;
import com.adem.attijari_compass.dto.card.LinkCardRequest;
import com.adem.attijari_compass.dto.card.UserCardResponse;
import com.adem.attijari_compass.dto.card.UserCardLinkResponse;
import com.adem.attijari_compass.service.CardService;
import com.adem.attijari_compass.service.SandboxCardGenerationService;
import com.adem.attijari_compass.service.card.CardApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final SandboxCardGenerationService sandboxCardGenerationService;
    private final CardApplicationService cardApplicationService;

    @PostMapping("/test/connect")
    public ResponseEntity<CardSyncResponse> connectTestCard(
            @Valid @RequestBody ConnectTestCardRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cardService.connectTestCard(request, userDetails.getUsername()));
    }

    @PostMapping("/test/{testCardId}/connect")
    public ResponseEntity<CardSyncResponse> connectGeneratedTestCard(
            @PathVariable Long testCardId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cardService.connectTestCardById(testCardId, userDetails.getUsername()));
    }

    @PostMapping("/test/generate")
    public ResponseEntity<GenerateTestCardResponse> generateTestCard(
            @Valid @RequestBody GenerateTestCardRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String currentUserEmail = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sandboxCardGenerationService.generateSandboxCard(request, currentUserEmail));
    }

    @GetMapping("/my-cards")
    public ResponseEntity<List<CardSummaryDto>> getMyCards(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cardApplicationService.getCurrentUserCards(userDetails.getUsername()));
    }

    @GetMapping("/my-cards/{cardId}")
    public ResponseEntity<CardDetailsDto> getMyCardDetails(
            @PathVariable Long cardId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cardApplicationService.getCurrentUserCardDetails(userDetails.getUsername(), cardId));
    }

    @GetMapping("/my-cards/{cardId}/transactions")
    public ResponseEntity<List<CardTransactionDto>> getMyCardTransactions(
            @PathVariable Long cardId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cardApplicationService.getCurrentUserCardTransactions(userDetails.getUsername(), cardId));
    }

    @PostMapping({"/link", "/my-cards"})
    public ResponseEntity<UserCardLinkResponse> linkCard(
            @Valid @RequestBody LinkCardRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cardApplicationService.linkCurrentUserCard(userDetails.getUsername(), request));
    }

    @DeleteMapping("/my-cards/{cardId}")
    public ResponseEntity<Void> unlinkMyCard(
            @PathVariable Long cardId,
            @AuthenticationPrincipal UserDetails userDetails) {
        cardApplicationService.unlinkCurrentUserCard(userDetails.getUsername(), cardId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/catalog")
    public ResponseEntity<List<CardCatalogDto>> getCardCatalog() {
        return ResponseEntity.ok(cardApplicationService.getCardCatalog());
    }

    @GetMapping("/test/my-cards")
    public ResponseEntity<List<UserCardResponse>> getSandboxMyCards(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cardService.getCurrentUserCards(userDetails.getUsername()));
    }

    @GetMapping("/{cardId}/transactions")
    public ResponseEntity<List<CardTransactionResponse>> getCardTransactions(
            @PathVariable Long cardId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cardService.getCardTransactions(cardId, userDetails.getUsername()));
    }

    @PostMapping("/{cardId}/sync")
    public ResponseEntity<CardSyncResponse> syncCard(
            @PathVariable Long cardId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cardService.syncCard(cardId, userDetails.getUsername()));
    }

    @DeleteMapping("/{cardId}")
    public ResponseEntity<CardActionResponse> disconnectCard(
            @PathVariable Long cardId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cardService.disconnectCard(cardId, userDetails.getUsername()));
    }
}

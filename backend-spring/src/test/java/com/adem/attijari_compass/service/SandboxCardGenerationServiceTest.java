package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.card.CardSyncResponse;
import com.adem.attijari_compass.dto.card.GenerateTestCardRequest;
import com.adem.attijari_compass.dto.card.GenerateTestCardResponse;
import com.adem.attijari_compass.dto.card.UserCardResponse;
import com.adem.attijari_compass.entity.CardStatus;
import com.adem.attijari_compass.entity.CardType;
import com.adem.attijari_compass.entity.SandboxCardProfile;
import com.adem.attijari_compass.entity.SandboxTransactionType;
import com.adem.attijari_compass.entity.TestCardCatalog;
import com.adem.attijari_compass.entity.TestCardTransaction;
import com.adem.attijari_compass.exception.AuthenticationRequiredException;
import com.adem.attijari_compass.repository.TestCardCatalogRepository;
import com.adem.attijari_compass.repository.TestCardTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SandboxCardGenerationServiceTest {

    @Mock
    private TestCardCatalogRepository testCardCatalogRepository;

    @Mock
    private TestCardTransactionRepository testCardTransactionRepository;

    @Mock
    private CardService cardService;

    @InjectMocks
    private SandboxCardGenerationService sandboxCardGenerationService;

    @Test
    void shouldGenerateSandboxCardWithoutConnectingToCurrentUser() {
        GenerateTestCardRequest request = GenerateTestCardRequest.builder()
                .holderName("Zita Test")
                .profile(SandboxCardProfile.SALARIED)
                .transactionCount(30)
                .connectToCurrentUser(false)
                .build();

        when(testCardCatalogRepository.existsByTestCardNumber(any())).thenReturn(false);
        when(testCardCatalogRepository.save(any(TestCardCatalog.class))).thenAnswer(invocation -> {
            TestCardCatalog card = invocation.getArgument(0);
            card.setId(101L);
            return card;
        });

        GenerateTestCardResponse response = sandboxCardGenerationService.generateSandboxCard(request, null);

        assertEquals("Sandbox card generated successfully", response.getMessage());
        assertEquals(SandboxCardProfile.SALARIED, response.getProfile());
        assertEquals(30, response.getGeneratedTransactions());
        assertEquals(0, response.getImportedTransactions());
        assertTrue(!response.isConnectedToCurrentUser());
        assertNull(response.getConnectedCard());
        assertNull(response.getSyncedAt());
        assertNotNull(response.getCard());
        assertNotNull(response.getCard().getTestCardNumber());
        assertEquals(CardStatus.ACTIVE, response.getCard().getStatus());

        ArgumentCaptor<List> transactionCaptor = ArgumentCaptor.forClass(List.class);
        verify(testCardTransactionRepository).saveAll(transactionCaptor.capture());

        @SuppressWarnings("unchecked")
        List<TestCardTransaction> transactions = transactionCaptor.getValue();
        assertEquals(30, transactions.size());
        assertEquals(30, new HashSet<>(transactions.stream().map(TestCardTransaction::getExternalReference).toList()).size());
        verify(cardService, never()).connectGeneratedCard(any(), any());
    }

    @Test
    void shouldFailWhenAnonymousUserRequestsAutoConnection() {
        GenerateTestCardRequest request = GenerateTestCardRequest.builder()
                .holderName("Zita Test")
                .profile(SandboxCardProfile.STUDENT)
                .transactionCount(12)
                .connectToCurrentUser(true)
                .build();

        assertThrows(AuthenticationRequiredException.class,
                () -> sandboxCardGenerationService.generateSandboxCard(request, null));

        verify(testCardCatalogRepository, never()).save(any(TestCardCatalog.class));
    }

    @Test
    void shouldGenerateAndConnectSandboxCardForAuthenticatedUser() {
        GenerateTestCardRequest request = GenerateTestCardRequest.builder()
                .holderName("Leila Premium")
                .profile(SandboxCardProfile.PREMIUM)
                .transactionCount(18)
                .connectToCurrentUser(true)
                .build();

        when(testCardCatalogRepository.existsByTestCardNumber(any())).thenReturn(false);
        when(testCardCatalogRepository.save(any(TestCardCatalog.class))).thenAnswer(invocation -> {
            TestCardCatalog card = invocation.getArgument(0);
            card.setId(202L);
            return card;
        });
        when(cardService.connectGeneratedCard(any(TestCardCatalog.class), any()))
                .thenReturn(CardSyncResponse.builder()
                        .message("Sandbox card generated and connected successfully")
                        .card(UserCardResponse.builder()
                                .id(88L)
                                .linkedTestCardId(202L)
                                .holderName("LEILA PREMIUM")
                                .maskedCardNumber("**** **** **** 1234")
                                .cardType(CardType.MASTERCARD)
                                .bankName("Attijari Bank Tunisie")
                                .status(CardStatus.ACTIVE)
                                .connectedAt(LocalDateTime.now())
                                .active(true)
                                .build())
                        .importedTransactions(18)
                        .skippedTransactions(0)
                        .syncedAt(LocalDateTime.now())
                        .build());

        GenerateTestCardResponse response = sandboxCardGenerationService.generateSandboxCard(request, "premium@test.com");

        assertEquals("Sandbox card generated and connected successfully", response.getMessage());
        assertTrue(response.isConnectedToCurrentUser());
        assertEquals(18, response.getGeneratedTransactions());
        assertEquals(18, response.getImportedTransactions());
        assertNotNull(response.getConnectedCard());
        assertNotNull(response.getSyncedAt());
        verify(cardService).connectGeneratedCard(any(TestCardCatalog.class), any());
    }

    @Test
    void shouldGenerateCoherentTransactionTypesForEachProfile() {
        AtomicLong nextCardId = new AtomicLong(300L);
        AtomicReference<List<TestCardTransaction>> lastSavedTransactions = new AtomicReference<>(List.of());

        when(testCardCatalogRepository.existsByTestCardNumber(any())).thenReturn(false);
        when(testCardCatalogRepository.save(any(TestCardCatalog.class))).thenAnswer(invocation -> {
            TestCardCatalog card = invocation.getArgument(0);
            card.setId(nextCardId.incrementAndGet());
            return card;
        });
        when(testCardTransactionRepository.saveAll(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<TestCardTransaction> savedTransactions = new ArrayList<>((List<TestCardTransaction>) invocation.getArgument(0));
            lastSavedTransactions.set(savedTransactions);
            return invocation.getArgument(0);
        });

        assertProfileTransactionTypes(
                SandboxCardProfile.STUDENT,
                30,
                Set.of("Bourse Etudiante"),
                Set.of("Netflix", "Ooredoo", "Uber", "Foody", "Monoprix", "Bolt", "Carrefour City", "KFC Tunis", "Cinema"),
                lastSavedTransactions
        );
        assertProfileTransactionTypes(
                SandboxCardProfile.SALARIED,
                30,
                Set.of("Salaire"),
                Set.of("Loyer", "Orange", "Netflix", "Carrefour", "Uber", "Monoprix", "Zara", "Foody", "Pharmacie"),
                lastSavedTransactions
        );
        assertProfileTransactionTypes(
                SandboxCardProfile.FAMILY,
                30,
                Set.of("Salaire Foyer"),
                Set.of("Loyer", "Frais Scolaires", "STEG", "Carrefour", "Pharmacie", "Monoprix", "Maison", "MG", "Transport"),
                lastSavedTransactions
        );
        assertProfileTransactionTypes(
                SandboxCardProfile.PREMIUM,
                30,
                Set.of("Salaire Premium"),
                Set.of("Concierge", "Club Prive", "Restaurant Premium", "Uber Black", "Voyage", "Hotel", "Luxury Shopping", "Spa", "Gourmet Store"),
                lastSavedTransactions
        );
    }

    private void assertProfileTransactionTypes(
            SandboxCardProfile profile,
            int transactionCount,
            Set<String> creditMerchants,
            Set<String> requiredDebitMerchants,
            AtomicReference<List<TestCardTransaction>> lastSavedTransactions
    ) {
        GenerateTestCardRequest request = GenerateTestCardRequest.builder()
                .holderName(profile.name() + " TEST")
                .profile(profile)
                .transactionCount(transactionCount)
                .connectToCurrentUser(false)
                .build();

        sandboxCardGenerationService.generateSandboxCard(request, null);

        List<TestCardTransaction> generatedTransactions = lastSavedTransactions.get();
        Set<String> merchantNames = generatedTransactions.stream()
                .map(TestCardTransaction::getMerchantName)
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(transactionCount, generatedTransactions.size());
        assertTrue(merchantNames.containsAll(creditMerchants));
        assertTrue(merchantNames.containsAll(requiredDebitMerchants));
        assertTrue(generatedTransactions.stream()
                .filter(transaction -> creditMerchants.contains(transaction.getMerchantName()))
                .allMatch(transaction -> transaction.getTransactionType() == SandboxTransactionType.CREDIT));
        assertTrue(generatedTransactions.stream()
                .filter(transaction -> !creditMerchants.contains(transaction.getMerchantName()))
                .allMatch(transaction -> transaction.getTransactionType() == SandboxTransactionType.DEBIT));
    }
}

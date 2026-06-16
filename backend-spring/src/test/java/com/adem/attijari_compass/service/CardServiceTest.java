package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.card.CardTransactionResponse;
import com.adem.attijari_compass.dto.card.CardSyncResponse;
import com.adem.attijari_compass.dto.card.ConnectTestCardRequest;
import com.adem.attijari_compass.entity.CardStatus;
import com.adem.attijari_compass.entity.CardType;
import com.adem.attijari_compass.entity.Role;
import com.adem.attijari_compass.entity.SandboxTransactionType;
import com.adem.attijari_compass.entity.TestCardCatalog;
import com.adem.attijari_compass.entity.TestCardTransaction;
import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionSource;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.entity.UserCard;
import com.adem.attijari_compass.exception.CardAlreadyLinkedException;
import com.adem.attijari_compass.exception.SandboxCardNotFoundException;
import com.adem.attijari_compass.repository.TestCardCatalogRepository;
import com.adem.attijari_compass.repository.TestCardTransactionRepository;
import com.adem.attijari_compass.repository.TransactionRepository;
import com.adem.attijari_compass.repository.UserCardRepository;
import com.adem.attijari_compass.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private TestCardCatalogRepository testCardCatalogRepository;

    @Mock
    private TestCardTransactionRepository testCardTransactionRepository;

    @Mock
    private UserCardRepository userCardRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CardService cardService;

    @Test
    void shouldConnectSandboxCardAndImportTransactions() {
        User user = buildUser();
        TestCardCatalog testCard = buildTestCard();

        ConnectTestCardRequest request = ConnectTestCardRequest.builder()
                .cardNumber(testCard.getTestCardNumber())
                .holderName(testCard.getHolderName())
                .expiryMonth(testCard.getExpiryMonth())
                .expiryYear(testCard.getExpiryYear())
                .cvv(testCard.getCvv())
                .build();

        List<TestCardTransaction> sandboxTransactions = List.of(
                buildSandboxTransaction(testCard, "CARD-001", SandboxTransactionType.DEBIT, 54.2, TransactionCategory.ALIMENTATION),
                buildSandboxTransaction(testCard, "CARD-002", SandboxTransactionType.CREDIT, 1_200.0, TransactionCategory.AUTRES)
        );

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(testCardCatalogRepository.findByTestCardNumber(testCard.getTestCardNumber())).thenReturn(Optional.of(testCard));
        when(userCardRepository.findByUserIdAndLinkedTestCardId(user.getId(), testCard.getId())).thenReturn(Optional.empty());
        when(userCardRepository.save(any(UserCard.class))).thenAnswer(invocation -> {
            UserCard userCard = invocation.getArgument(0);
            if (userCard.getId() == null) {
                userCard.setId(44L);
            }
            return userCard;
        });
        when(testCardTransactionRepository.findByTestCardIdOrderByTransactionDateDesc(testCard.getId())).thenReturn(sandboxTransactions);
        when(transactionRepository.findExternalReferencesByUserIdAndUserCardId(user.getId(), 44L)).thenReturn(List.of());

        CardSyncResponse response = cardService.connectTestCard(request, user.getEmail());

        assertNotNull(response);
        assertEquals("Sandbox card connected successfully", response.getMessage());
        assertEquals(2, response.getImportedTransactions());
        assertEquals(0, response.getSkippedTransactions());
        assertEquals("**** **** **** 4242", response.getCard().getMaskedCardNumber());
        assertEquals(CardStatus.ACTIVE, response.getCard().getStatus());
        assertNotNull(response.getSyncedAt());

        ArgumentCaptor<List> transactionCaptor = ArgumentCaptor.forClass(List.class);
        verify(transactionRepository).saveAll(transactionCaptor.capture());

        @SuppressWarnings("unchecked")
        List<Transaction> savedTransactions = transactionCaptor.getValue();
        assertEquals(2, savedTransactions.size());
        assertEquals(TransactionSource.CARD_SANDBOX, savedTransactions.getFirst().getSource());
        assertEquals(TransactionType.DEPENSE, savedTransactions.getFirst().getType());
        assertEquals(TransactionType.REVENU, savedTransactions.get(1).getType());
        assertEquals("CARD-001", savedTransactions.getFirst().getExternalReference());
    }

    @Test
    void shouldConnectSandboxCardByIdAndImportTransactions() {
        User user = buildUser();
        TestCardCatalog testCard = buildTestCard();

        List<TestCardTransaction> sandboxTransactions = List.of(
                buildSandboxTransaction(testCard, "ID-001", SandboxTransactionType.DEBIT, 78.4, TransactionCategory.CAFES),
                buildSandboxTransaction(testCard, "ID-002", SandboxTransactionType.CREDIT, 950.0, TransactionCategory.BANQUE)
        );

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(testCardCatalogRepository.findById(testCard.getId())).thenReturn(Optional.of(testCard));
        when(userCardRepository.findByUserIdAndLinkedTestCardId(user.getId(), testCard.getId())).thenReturn(Optional.empty());
        when(userCardRepository.save(any(UserCard.class))).thenAnswer(invocation -> {
            UserCard userCard = invocation.getArgument(0);
            if (userCard.getId() == null) {
                userCard.setId(55L);
            }
            return userCard;
        });
        when(testCardTransactionRepository.findByTestCardIdOrderByTransactionDateDesc(testCard.getId())).thenReturn(sandboxTransactions);
        when(transactionRepository.findExternalReferencesByUserIdAndUserCardId(user.getId(), 55L)).thenReturn(List.of());

        CardSyncResponse response = cardService.connectTestCardById(testCard.getId(), user.getEmail());

        assertNotNull(response);
        assertEquals("Sandbox card connected successfully", response.getMessage());
        assertEquals(2, response.getImportedTransactions());
        assertEquals(0, response.getSkippedTransactions());
        assertEquals(testCard.getId(), response.getCard().getLinkedTestCardId());

        ArgumentCaptor<List> transactionCaptor = ArgumentCaptor.forClass(List.class);
        verify(transactionRepository).saveAll(transactionCaptor.capture());

        @SuppressWarnings("unchecked")
        List<Transaction> savedTransactions = transactionCaptor.getValue();
        assertEquals(TransactionType.DEPENSE, savedTransactions.getFirst().getType());
        assertEquals(TransactionType.REVENU, savedTransactions.get(1).getType());
    }

    @Test
    void shouldRejectAlreadyLinkedCard() {
        User user = buildUser();
        TestCardCatalog testCard = buildTestCard();
        UserCard existingCard = UserCard.builder()
                .id(77L)
                .user(user)
                .linkedTestCard(testCard)
                .maskedCardNumber(testCard.getMaskedCardNumber())
                .holderName(testCard.getHolderName())
                .cardType(testCard.getCardType())
                .bankName(testCard.getBankName())
                .status(CardStatus.ACTIVE)
                .connectedAt(LocalDateTime.now().minusDays(10))
                .lastSyncAt(LocalDateTime.now().minusDays(1))
                .isActive(true)
                .build();

        ConnectTestCardRequest request = ConnectTestCardRequest.builder()
                .cardNumber(testCard.getTestCardNumber())
                .holderName(testCard.getHolderName())
                .expiryMonth(testCard.getExpiryMonth())
                .expiryYear(testCard.getExpiryYear())
                .cvv(testCard.getCvv())
                .build();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(testCardCatalogRepository.findByTestCardNumber(testCard.getTestCardNumber())).thenReturn(Optional.of(testCard));
        when(userCardRepository.findByUserIdAndLinkedTestCardId(user.getId(), testCard.getId()))
                .thenReturn(Optional.of(existingCard));

        assertThrows(CardAlreadyLinkedException.class, () -> cardService.connectTestCard(request, user.getEmail()));

        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    void shouldRejectMissingSandboxCardWhenConnectingById() {
        User user = buildUser();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(testCardCatalogRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(SandboxCardNotFoundException.class, () -> cardService.connectTestCardById(999L, user.getEmail()));

        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    void shouldRejectAlreadyLinkedCardWhenConnectingById() {
        User user = buildUser();
        TestCardCatalog testCard = buildTestCard();
        UserCard existingCard = UserCard.builder()
                .id(78L)
                .user(user)
                .linkedTestCard(testCard)
                .maskedCardNumber(testCard.getMaskedCardNumber())
                .holderName(testCard.getHolderName())
                .cardType(testCard.getCardType())
                .bankName(testCard.getBankName())
                .status(CardStatus.ACTIVE)
                .connectedAt(LocalDateTime.now().minusDays(12))
                .lastSyncAt(LocalDateTime.now().minusDays(1))
                .isActive(true)
                .build();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(testCardCatalogRepository.findById(testCard.getId())).thenReturn(Optional.of(testCard));
        when(userCardRepository.findByUserIdAndLinkedTestCardId(user.getId(), testCard.getId()))
                .thenReturn(Optional.of(existingCard));

        assertThrows(CardAlreadyLinkedException.class, () -> cardService.connectTestCardById(testCard.getId(), user.getEmail()));

        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    void shouldSkipAlreadyImportedTransactionsDuringSync() {
        User user = buildUser();
        TestCardCatalog testCard = buildTestCard();
        UserCard userCard = UserCard.builder()
                .id(88L)
                .user(user)
                .linkedTestCard(testCard)
                .maskedCardNumber(testCard.getMaskedCardNumber())
                .holderName(testCard.getHolderName())
                .cardType(testCard.getCardType())
                .bankName(testCard.getBankName())
                .status(CardStatus.ACTIVE)
                .connectedAt(LocalDateTime.now().minusDays(20))
                .lastSyncAt(LocalDateTime.now().minusDays(2))
                .isActive(true)
                .build();

        List<TestCardTransaction> sandboxTransactions = List.of(
                buildSandboxTransaction(testCard, "SYNC-001", SandboxTransactionType.DEBIT, 18.5, TransactionCategory.TRANSPORT),
                buildSandboxTransaction(testCard, "SYNC-002", SandboxTransactionType.DEBIT, 72.4, TransactionCategory.STEG_SONEDE)
        );

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userCardRepository.findByIdAndUserId(userCard.getId(), user.getId())).thenReturn(Optional.of(userCard));
        when(testCardTransactionRepository.findByTestCardIdOrderByTransactionDateDesc(testCard.getId())).thenReturn(sandboxTransactions);
        when(transactionRepository.findExternalReferencesByUserIdAndUserCardId(user.getId(), userCard.getId()))
                .thenReturn(List.of("SYNC-001"));
        when(userCardRepository.save(any(UserCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CardSyncResponse response = cardService.syncCard(userCard.getId(), user.getEmail());

        assertEquals(1, response.getImportedTransactions());
        assertEquals(1, response.getSkippedTransactions());

        ArgumentCaptor<List> transactionCaptor = ArgumentCaptor.forClass(List.class);
        verify(transactionRepository).saveAll(transactionCaptor.capture());

        @SuppressWarnings("unchecked")
        List<Transaction> savedTransactions = transactionCaptor.getValue();
        assertEquals(1, savedTransactions.size());
        assertEquals("SYNC-002", savedTransactions.getFirst().getExternalReference());
    }

    @Test
    void shouldDisconnectLinkedCard() {
        User user = buildUser();
        TestCardCatalog testCard = buildTestCard();
        UserCard userCard = UserCard.builder()
                .id(19L)
                .user(user)
                .linkedTestCard(testCard)
                .maskedCardNumber(testCard.getMaskedCardNumber())
                .holderName(testCard.getHolderName())
                .cardType(testCard.getCardType())
                .bankName(testCard.getBankName())
                .status(CardStatus.ACTIVE)
                .connectedAt(LocalDateTime.now().minusDays(5))
                .isActive(true)
                .build();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userCardRepository.findByIdAndUserId(userCard.getId(), user.getId())).thenReturn(Optional.of(userCard));
        when(userCardRepository.save(any(UserCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = cardService.disconnectCard(userCard.getId(), user.getEmail());

        assertEquals(userCard.getId(), response.getCardId());
        assertFalse(response.isActive());
        assertEquals(CardStatus.INACTIVE, userCard.getStatus());
    }

    @Test
    void shouldExposeExplicitTransactionTypeWhenListingCardTransactions() {
        User user = buildUser();
        TestCardCatalog testCard = buildTestCard();
        UserCard userCard = UserCard.builder()
                .id(91L)
                .user(user)
                .linkedTestCard(testCard)
                .maskedCardNumber(testCard.getMaskedCardNumber())
                .holderName(testCard.getHolderName())
                .cardType(testCard.getCardType())
                .bankName(testCard.getBankName())
                .status(CardStatus.ACTIVE)
                .connectedAt(LocalDateTime.now().minusDays(8))
                .isActive(true)
                .build();

        List<Transaction> transactions = List.of(
                Transaction.builder()
                        .id(1001L)
                        .description("Sejour hotel")
                        .merchantName("Hotel")
                        .amount(680.0)
                        .date(LocalDateTime.now().minusDays(2).toLocalDate())
                        .category(TransactionCategory.HOTEL)
                        .type(TransactionType.DEPENSE)
                        .source(TransactionSource.CARD_SANDBOX)
                        .cardLast4("4242")
                        .externalReference("PREM-001")
                        .createdAt(LocalDateTime.now().minusHours(5))
                        .user(user)
                        .userCard(userCard)
                        .build(),
                Transaction.builder()
                        .id(1002L)
                        .description("Salaire premium")
                        .merchantName("Salaire Premium")
                        .amount(12_800.0)
                        .date(LocalDateTime.now().minusDays(1).toLocalDate())
                        .category(TransactionCategory.AUTRES)
                        .type(TransactionType.REVENU)
                        .source(TransactionSource.CARD_SANDBOX)
                        .cardLast4("4242")
                        .externalReference("PREM-002")
                        .createdAt(LocalDateTime.now().minusHours(2))
                        .user(user)
                        .userCard(userCard)
                        .build()
        );

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userCardRepository.findByIdAndUserId(userCard.getId(), user.getId())).thenReturn(Optional.of(userCard));
        when(transactionRepository.findAllByUserIdAndUserCardId(user.getId(), userCard.getId())).thenReturn(transactions);

        List<CardTransactionResponse> response = cardService.getCardTransactions(userCard.getId(), user.getEmail());

        assertEquals(2, response.size());
        assertEquals(SandboxTransactionType.DEBIT, response.getFirst().getTransactionType());
        assertEquals(TransactionType.DEPENSE, response.getFirst().getType());
        assertEquals(SandboxTransactionType.CREDIT, response.get(1).getTransactionType());
        assertEquals(TransactionType.REVENU, response.get(1).getType());
        assertEquals(TransactionSource.CARD_SANDBOX, response.getFirst().getSource());
    }

    private User buildUser() {
        return User.builder()
                .id(7L)
                .email("cards@test.com")
                .password("secret")
                .role(Role.USER)
                .build();
    }

    private TestCardCatalog buildTestCard() {
        return TestCardCatalog.builder()
                .id(9L)
                .holderName("Test User")
                .maskedCardNumber("**** **** **** 4242")
                .testCardNumber("4242424242424242")
                .expiryMonth(12)
                .expiryYear(2099)
                .cvv("123")
                .cardType(CardType.VISA)
                .bankName("Attijari Bank Tunisie")
                .status(CardStatus.ACTIVE)
                .initialBalance(1_250.0)
                .build();
    }

    private TestCardTransaction buildSandboxTransaction(
            TestCardCatalog testCard,
            String externalReference,
            SandboxTransactionType transactionType,
            double amount,
            TransactionCategory category) {
        return TestCardTransaction.builder()
                .id(Math.abs(externalReference.hashCode()) + 0L)
                .testCard(testCard)
                .merchantName("Sandbox Merchant")
                .rawLabel("RAW " + externalReference)
                .amount(amount)
                .transactionType(transactionType)
                .transactionDate(LocalDateTime.now().minusDays(3))
                .categorySuggestion(category)
                .description("Description " + externalReference)
                .externalReference(externalReference)
                .build();
    }
}

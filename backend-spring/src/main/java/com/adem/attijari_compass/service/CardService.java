package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.card.CardActionResponse;
import com.adem.attijari_compass.dto.card.CardSyncResponse;
import com.adem.attijari_compass.dto.card.CardTransactionResponse;
import com.adem.attijari_compass.dto.card.ConnectTestCardRequest;
import com.adem.attijari_compass.dto.card.UserCardResponse;
import com.adem.attijari_compass.entity.CardStatus;
import com.adem.attijari_compass.entity.PaymentMethod;
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
import com.adem.attijari_compass.exception.CardExpiredException;
import com.adem.attijari_compass.exception.CardInactiveException;
import com.adem.attijari_compass.exception.InvalidCardCredentialsException;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.exception.SandboxCardNotFoundException;
import com.adem.attijari_compass.repository.TestCardCatalogRepository;
import com.adem.attijari_compass.repository.TestCardTransactionRepository;
import com.adem.attijari_compass.repository.TransactionRepository;
import com.adem.attijari_compass.repository.UserCardRepository;
import com.adem.attijari_compass.repository.UserRepository;
import com.adem.attijari_compass.util.CardMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CardService {

    private final TestCardCatalogRepository testCardCatalogRepository;
    private final TestCardTransactionRepository testCardTransactionRepository;
    private final UserCardRepository userCardRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public CardSyncResponse connectTestCard(ConnectTestCardRequest request, String email) {
        User user = getRequiredUser(email);
        TestCardCatalog testCard = resolveSandboxCard(request);
        validateCardCredentials(request, testCard);

        return connectCatalogCard(user, testCard, "Sandbox card connected successfully");
    }

    public CardSyncResponse connectTestCardById(Long testCardId, String email) {
        User user = getRequiredUser(email);
        TestCardCatalog testCard = getTestCardOrThrow(testCardId);
        ensureCardCanBeUsed(testCard);

        return connectCatalogCard(user, testCard, "Sandbox card connected successfully");
    }

    public CardSyncResponse connectGeneratedCard(TestCardCatalog testCard, String email) {
        User user = getRequiredUser(email);
        ensureCardCanBeUsed(testCard);
        return connectCatalogCard(user, testCard, "Sandbox card generated and connected successfully");
    }

    @Transactional(readOnly = true)
    public User getRequiredUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private CardSyncResponse connectCatalogCard(User user, TestCardCatalog testCard, String message) {
        UserCard userCard = userCardRepository.findByUserIdAndLinkedTestCardId(user.getId(), testCard.getId())
                .map(existingCard -> reactivateCard(existingCard, testCard))
                .orElseGet(() -> buildUserCard(user, testCard));

        userCard = userCardRepository.save(userCard);
        SyncSummary summary = syncTransactions(user, userCard);

        log.info("Sandbox card {} connected for user {}", userCard.getMaskedCardNumber(), user.getEmail());
        return buildSyncResponse(message, userCard, summary);
    }

    @Transactional(readOnly = true)
    public List<UserCardResponse> getCurrentUserCards(String email) {
        User user = getRequiredUser(email);
        return userCardRepository.findAllByUserIdOrderByConnectedAtDesc(user.getId()).stream()
                .map(this::mapToUserCardResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CardTransactionResponse> getCardTransactions(Long cardId, String email) {
        User user = getRequiredUser(email);
        UserCard userCard = getUserCardOrThrow(cardId, user.getId());

        return transactionRepository.findAllByUserIdAndUserCardId(user.getId(), userCard.getId()).stream()
                .map(transaction -> mapToCardTransactionResponse(transaction, userCard))
                .toList();
    }

    public CardSyncResponse syncCard(Long cardId, String email) {
        User user = getRequiredUser(email);
        UserCard userCard = getUserCardOrThrow(cardId, user.getId());

        if (!userCard.isActive()) {
            throw new CardInactiveException("This card is disconnected and cannot be synchronized");
        }

        ensureCardCanBeUsed(userCard.getLinkedTestCard());
        SyncSummary summary = syncTransactions(user, userCard);

        log.info("Sandbox card {} synchronized for user {}", userCard.getMaskedCardNumber(), email);
        return buildSyncResponse("Sandbox card synchronized successfully", userCard, summary);
    }

    public CardActionResponse disconnectCard(Long cardId, String email) {
        User user = getRequiredUser(email);
        UserCard userCard = getUserCardOrThrow(cardId, user.getId());

        userCard.setActive(false);
        userCard.setStatus(CardStatus.INACTIVE);
        userCardRepository.save(userCard);

        log.info("Sandbox card {} disconnected for user {}", userCard.getMaskedCardNumber(), email);
        return CardActionResponse.builder()
                .message("Sandbox card disconnected successfully")
                .cardId(userCard.getId())
                .active(false)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private TestCardCatalog resolveSandboxCard(ConnectTestCardRequest request) {
        String normalizedCardNumber = CardMaskingUtil.normalizeCardNumber(request.getCardNumber());
        if (!StringUtils.hasText(normalizedCardNumber)) {
            throw new InvalidCardCredentialsException("Card number is invalid");
        }

        return testCardCatalogRepository.findByTestCardNumber(normalizedCardNumber)
                .orElseThrow(() -> new SandboxCardNotFoundException("Sandbox test card not found"));
    }

    private void validateCardCredentials(ConnectTestCardRequest request, TestCardCatalog testCard) {
        if (!normalizeHolderName(request.getHolderName()).equals(normalizeHolderName(testCard.getHolderName()))
                || !Objects.equals(request.getExpiryMonth(), testCard.getExpiryMonth())
                || !Objects.equals(request.getExpiryYear(), testCard.getExpiryYear())
                || !request.getCvv().trim().equals(testCard.getCvv())) {
            throw new InvalidCardCredentialsException("Sandbox card credentials are invalid");
        }

        ensureCardCanBeUsed(testCard);
    }

    private void ensureCardCanBeUsed(TestCardCatalog testCard) {
        if (testCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardInactiveException("Sandbox card is not active");
        }

        YearMonth expiryDate = YearMonth.of(testCard.getExpiryYear(), testCard.getExpiryMonth());
        if (expiryDate.isBefore(YearMonth.now())) {
            throw new CardExpiredException("Sandbox card is expired");
        }
    }

    private TestCardCatalog getTestCardOrThrow(Long testCardId) {
        return testCardCatalogRepository.findById(testCardId)
                .orElseThrow(() -> new SandboxCardNotFoundException("Sandbox test card not found"));
    }

    private UserCard reactivateCard(UserCard existingCard, TestCardCatalog testCard) {
        if (existingCard.isActive()) {
            throw new CardAlreadyLinkedException("This sandbox card is already linked to the current user");
        }

        existingCard.setMaskedCardNumber(testCard.getMaskedCardNumber());
        existingCard.setHolderName(testCard.getHolderName());
        existingCard.setCardType(testCard.getCardType());
        existingCard.setBankName(testCard.getBankName());
        existingCard.setStatus(CardStatus.ACTIVE);
        existingCard.setActive(true);
        existingCard.setConnectedAt(LocalDateTime.now());
        return existingCard;
    }

    private UserCard buildUserCard(User user, TestCardCatalog testCard) {
        return UserCard.builder()
                .user(user)
                .linkedTestCard(testCard)
                .maskedCardNumber(testCard.getMaskedCardNumber())
                .holderName(testCard.getHolderName())
                .cardType(testCard.getCardType())
                .bankName(testCard.getBankName())
                .status(CardStatus.ACTIVE)
                .connectedAt(LocalDateTime.now())
                .isActive(true)
                .build();
    }

    private SyncSummary syncTransactions(User user, UserCard userCard) {
        List<TestCardTransaction> sandboxTransactions =
                testCardTransactionRepository.findByTestCardIdOrderByTransactionDateDesc(userCard.getLinkedTestCard().getId());

        Set<String> existingReferences = new HashSet<>(
                transactionRepository.findExternalReferencesByUserIdAndUserCardId(user.getId(), userCard.getId())
        );

        List<Transaction> importedTransactions = new ArrayList<>();
        int skippedTransactions = 0;

        for (TestCardTransaction sandboxTransaction : sandboxTransactions) {
            String externalReference = sandboxTransaction.getExternalReference();
            if (StringUtils.hasText(externalReference) && existingReferences.contains(externalReference)) {
                skippedTransactions++;
                continue;
            }

            importedTransactions.add(buildImportedTransaction(user, userCard, sandboxTransaction));
            if (StringUtils.hasText(externalReference)) {
                existingReferences.add(externalReference);
            }
        }

        if (!importedTransactions.isEmpty()) {
            transactionRepository.saveAll(importedTransactions);
        }

        LocalDateTime syncedAt = LocalDateTime.now();
        userCard.setLastSyncAt(syncedAt);
        userCardRepository.save(userCard);

        return new SyncSummary(importedTransactions.size(), skippedTransactions, syncedAt);
    }

    private Transaction buildImportedTransaction(User user, UserCard userCard, TestCardTransaction sandboxTransaction) {
        return Transaction.builder()
                .description(resolveImportedDescription(sandboxTransaction))
                .amount(sandboxTransaction.getAmount())
                .date(sandboxTransaction.getTransactionDate().toLocalDate())
                .category(resolveCategory(sandboxTransaction))
                .type(mapTransactionType(sandboxTransaction.getTransactionType()))
                .merchantName(sandboxTransaction.getMerchantName())
                .paymentMethod(PaymentMethod.CARD)
                .source(TransactionSource.CARD_SANDBOX)
                .cardLast4(CardMaskingUtil.extractLast4(userCard.getMaskedCardNumber()))
                .externalReference(sandboxTransaction.getExternalReference())
                .createdAt(LocalDateTime.now())
                .user(user)
                .userCard(userCard)
                .build();
    }

    private String resolveImportedDescription(TestCardTransaction sandboxTransaction) {
        if (StringUtils.hasText(sandboxTransaction.getDescription())) {
            return sandboxTransaction.getDescription();
        }
        return sandboxTransaction.getRawLabel();
    }

    private TransactionCategory resolveCategory(TestCardTransaction sandboxTransaction) {
        return sandboxTransaction.getCategorySuggestion() != null
                ? sandboxTransaction.getCategorySuggestion()
                : TransactionCategory.fallback();
    }

    private TransactionType mapTransactionType(SandboxTransactionType sandboxTransactionType) {
        return sandboxTransactionType == SandboxTransactionType.CREDIT
                ? TransactionType.REVENU
                : TransactionType.DEPENSE;
    }

    private UserCard getUserCardOrThrow(Long cardId, Long userId) {
        return userCardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Linked card not found with id: " + cardId));
    }

    private CardSyncResponse buildSyncResponse(String message, UserCard userCard, SyncSummary summary) {
        return CardSyncResponse.builder()
                .message(message)
                .card(mapToUserCardResponse(userCard))
                .importedTransactions(summary.importedTransactions())
                .skippedTransactions(summary.skippedTransactions())
                .syncedAt(summary.syncedAt())
                .build();
    }

    private UserCardResponse mapToUserCardResponse(UserCard userCard) {
        return UserCardResponse.builder()
                .id(userCard.getId())
                .linkedTestCardId(userCard.getLinkedTestCard().getId())
                .holderName(userCard.getHolderName())
                .maskedCardNumber(userCard.getMaskedCardNumber())
                .cardType(userCard.getCardType())
                .bankName(userCard.getBankName())
                .status(userCard.getStatus())
                .connectedAt(userCard.getConnectedAt())
                .lastSyncAt(userCard.getLastSyncAt())
                .active(userCard.isActive())
                .build();
    }

    private CardTransactionResponse mapToCardTransactionResponse(Transaction transaction, UserCard userCard) {
        return CardTransactionResponse.builder()
                .id(transaction.getId())
                .merchantName(transaction.getMerchantName())
                .description(transaction.getDescription())
                .amount(transaction.getAmount())
                .transactionType(mapResponseTransactionType(transaction.getType()))
                .type(transaction.getType())
                .date(transaction.getDate())
                .category(transaction.getCategory())
                .source(transaction.getSource())
                .maskedCardNumber(userCard.getMaskedCardNumber())
                .cardLast4(transaction.getCardLast4())
                .externalReference(transaction.getExternalReference())
                .importedAt(transaction.getCreatedAt())
                .build();
    }

    private SandboxTransactionType mapResponseTransactionType(TransactionType transactionType) {
        return transactionType == TransactionType.REVENU
                ? SandboxTransactionType.CREDIT
                : SandboxTransactionType.DEBIT;
    }

    private String normalizeHolderName(String holderName) {
        return holderName == null
                ? ""
                : holderName.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private record SyncSummary(int importedTransactions, int skippedTransactions, LocalDateTime syncedAt) {
    }
}

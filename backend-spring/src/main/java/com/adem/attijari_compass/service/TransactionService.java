package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.transaction.CardPaymentRequest;
import com.adem.attijari_compass.dto.transaction.PaginatedTransactionResponse;
import com.adem.attijari_compass.dto.transaction.TransactionRequest;
import com.adem.attijari_compass.dto.transaction.TransactionBulkDeleteResponse;
import com.adem.attijari_compass.dto.transaction.TransactionResponse;
import com.adem.attijari_compass.dto.transaction.UpdateCategoryRequest;
import com.adem.attijari_compass.entity.PaymentMethod;
import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionSource;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.entity.UserCard;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.model.categorization.CategorizationResult;
import com.adem.attijari_compass.repository.TransactionRepository;
import com.adem.attijari_compass.repository.TransactionCategoryFeedbackRepository;
import com.adem.attijari_compass.repository.UserCardRepository;
import com.adem.attijari_compass.repository.UserRepository;
import com.adem.attijari_compass.util.CardMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final UserCardRepository userCardRepository;
    private final SmartCategorizationService smartCategorizationService;
    private final TransactionCategoryFeedbackService transactionCategoryFeedbackService;
    private final TransactionCategoryFeedbackRepository transactionCategoryFeedbackRepository;
    private final NotificationCenterService notificationCenterService;

    public TransactionResponse createCardPayment(CardPaymentRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        CategorizationResult categorizationResult = smartCategorizationService.categorize(
                request.getMerchantName(),
                request.getDescription() != null ? request.getDescription() : "",
                user.getId()
        );

        Transaction transaction = Transaction.builder()
                .merchantName(request.getMerchantName())
                .description(request.getDescription() != null ? request.getDescription() : request.getMerchantName())
                .amount(request.getAmount())
                .date(request.getDate())
                .category(categorizationResult.getCategory())
                .type(TransactionType.DEPENSE)
                .paymentMethod(PaymentMethod.CARD)
                .source(TransactionSource.MANUAL_CARD)
                .cardLast4(request.getCardLast4())
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();

        applyCategorizationMetadata(transaction, categorizationResult);
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info(
                "Transaction created: userId={}, category={}, amount={}, type={}, transactionId={}",
                user.getId(),
                savedTransaction.getCategory(),
                savedTransaction.getAmount(),
                savedTransaction.getType(),
                savedTransaction.getId()
        );
        notificationCenterService.synchronizeBudgetNotificationsAfterExpense(user, savedTransaction);
        log.info(
                "Notification synchronization completed before transaction response returned: userId={}, transactionId={}",
                user.getId(),
                savedTransaction.getId()
        );
        return mapToResponse(savedTransaction);
    }

    public TransactionResponse createTransaction(TransactionRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        TransactionCategory category = request.getCategory();
        CategorizationResult categorizationResult = resolveRequestCategorizationMetadata(request);
        TransactionCategory predictedCategory = request.getPredictedCategory();
        if (category == null) {
            categorizationResult = smartCategorizationService.categorize(
                    request.getMerchantName(),
                    request.getDescription(),
                    user.getId()
            );
            category = categorizationResult.getCategory();
            predictedCategory = category;
        }

        PaymentMethod paymentMethod = resolvePaymentMethod(request);
        UserCard userCard = resolveUserCard(request.getUserCardId(), paymentMethod, user);

        Transaction transaction = Transaction.builder()
                .description(request.getDescription())
                .merchantName(request.getMerchantName())
                .amount(request.getAmount())
                .date(request.getDate())
                .category(category)
                .type(request.getType())
                .paymentMethod(paymentMethod)
                .source(userCard != null ? TransactionSource.MANUAL_CARD : TransactionSource.MANUAL_ENTRY)
                .cardLast4(userCard != null ? CardMaskingUtil.extractLast4(userCard.getMaskedCardNumber()) : null)
                .createdAt(LocalDateTime.now())
                .user(user)
                .userCard(userCard)
                .build();

        applyCategorizationMetadata(transaction, categorizationResult);
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info(
                "Transaction created: userId={}, category={}, amount={}, type={}, transactionId={}",
                user.getId(),
                savedTransaction.getCategory(),
                savedTransaction.getAmount(),
                savedTransaction.getType(),
                savedTransaction.getId()
        );
        if (savedTransaction.getType() == TransactionType.DEPENSE) {
            notificationCenterService.synchronizeBudgetNotificationsAfterExpense(user, savedTransaction);
            log.info(
                    "Notification synchronization completed before transaction response returned: userId={}, transactionId={}",
                    user.getId(),
                    savedTransaction.getId()
            );
        }
        recordCorrectionFromRequest(savedTransaction, request, predictedCategory, category, user.getId());
        return mapToResponse(savedTransaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAllTransactions(String email) {
        try {
            log.info("Retrieving transactions for user: {}", email);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
            log.debug("User found with ID: {}", user.getId());

            List<Transaction> transactions = transactionRepository.findAllByUserId(user.getId());
            log.debug("Found {} transactions for user {}", transactions.size(), user.getId());

            List<TransactionResponse> responses = transactions.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

            log.info("Successfully retrieved {} transactions for user {}", responses.size(), email);
            return responses;
        } catch (ResourceNotFoundException e) {
            log.error("User not found: {}", email);
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving transactions for user {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve transactions: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public PaginatedTransactionResponse getAllTransactionsPaginated(String email, Pageable pageable) {
        try {
            log.info("Retrieving paginated transactions for user: {} with page: {}, size: {}",
                    email, pageable.getPageNumber(), pageable.getPageSize());

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
            log.debug("User found with ID: {}", user.getId());

            Page<Transaction> transactionPage = transactionRepository.findAllByUserIdPaginated(user.getId(), pageable);
            log.debug("Found {} transactions for user {} (page {} of {})",
                    transactionPage.getContent().size(), user.getId(),
                    transactionPage.getNumber() + 1, transactionPage.getTotalPages());

            List<TransactionResponse> responses = transactionPage.getContent().stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

            PaginatedTransactionResponse paginatedResponse = PaginatedTransactionResponse.builder()
                    .content(responses)
                    .pageNumber(transactionPage.getNumber())
                    .pageSize(transactionPage.getSize())
                    .totalElements(transactionPage.getTotalElements())
                    .totalPages(transactionPage.getTotalPages())
                    .last(transactionPage.isLast())
                    .first(transactionPage.isFirst())
                    .build();

            log.info("Successfully retrieved paginated transactions for user {}", email);
            return paginatedResponse;
        } catch (ResourceNotFoundException e) {
            log.error("User not found: {}", email);
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving paginated transactions for user {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve paginated transactions: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long id, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        return mapToResponse(transaction);
    }

    public TransactionResponse updateTransaction(Long id, TransactionRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        TransactionCategory category = request.getCategory();
        CategorizationResult categorizationResult = resolveRequestCategorizationMetadata(request);
        TransactionCategory predictedCategory = request.getPredictedCategory();
        if (category == null) {
            categorizationResult = smartCategorizationService.categorize(
                    request.getMerchantName() != null ? request.getMerchantName() : transaction.getMerchantName(),
                    request.getDescription(),
                    user.getId()
            );
            category = categorizationResult.getCategory();
            predictedCategory = category;
        }

        PaymentMethod paymentMethod = resolvePaymentMethod(request);
        UserCard userCard = resolveUserCard(request.getUserCardId(), paymentMethod, user);

        transaction.setMerchantName(request.getMerchantName());
        transaction.setDescription(request.getDescription());
        transaction.setAmount(request.getAmount());
        transaction.setDate(request.getDate());
        transaction.setCategory(category);
        transaction.setType(request.getType());
        transaction.setPaymentMethod(paymentMethod);
        transaction.setUserCard(userCard);
        transaction.setSource(userCard != null ? TransactionSource.MANUAL_CARD : TransactionSource.MANUAL_ENTRY);
        transaction.setCardLast4(userCard != null ? CardMaskingUtil.extractLast4(userCard.getMaskedCardNumber()) : null);
        applyCategorizationMetadata(transaction, categorizationResult);
        Transaction savedTransaction = transactionRepository.save(transaction);
        recordCorrectionFromRequest(savedTransaction, request, predictedCategory, category, user.getId());
        return mapToResponse(savedTransaction);
    }

    public TransactionResponse updateCategory(Long id, UpdateCategoryRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        transactionCategoryFeedbackService.recordCorrection(transaction, request.getCategory(), user.getId());
        transaction.setCategory(request.getCategory());
        return mapToResponse(transactionRepository.save(transaction));
    }

    public void deleteTransaction(Long id, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        transactionRepository.delete(transaction);
    }

    public TransactionBulkDeleteResponse deleteAllCurrentUserTransactions(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        int deletedFeedbackCount = transactionCategoryFeedbackRepository.deleteAllByUserId(user.getId());
        int deletedTransactions = transactionRepository.deleteAllByUserId(user.getId());

        log.info(
                "Deleted {} transactions and {} feedback rows from main transaction store for user {}",
                deletedTransactions,
                deletedFeedbackCount,
                email
        );

        String message = deletedTransactions > 0
                ? "Toutes les transactions ont ete supprimees avec succes."
                : "Aucune transaction a supprimer pour l'utilisateur connecte.";

        return TransactionBulkDeleteResponse.builder()
                .message(message)
                .deletedCount(deletedTransactions)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public TransactionResponse mapToResponse(Transaction transaction) {
        try {
            if (transaction == null) {
                log.error("Transaction is null");
                throw new IllegalArgumentException("Transaction cannot be null");
            }

            Long userId = null;
            if (transaction.getUser() != null) {
                userId = transaction.getUser().getId();
            }

            UserCard userCard = transaction.getUserCard();

            TransactionResponse response = TransactionResponse.builder()
                    .id(transaction.getId())
                    .description(transaction.getDescription())
                    .amount(transaction.getAmount())
                    .date(transaction.getDate())
                    .category(transaction.getCategory())
                    .type(transaction.getType())
                    .userId(userId)
                    .merchantName(transaction.getMerchantName())
                    .paymentMethod(transaction.getPaymentMethod())
                    .source(transaction.getSource())
                    .cardLast4(transaction.getCardLast4())
                    .maskedCardNumber(userCard != null ? userCard.getMaskedCardNumber() : null)
                    .userCardId(userCard != null ? userCard.getId() : null)
                    .externalReference(transaction.getExternalReference())
                    .createdAt(transaction.getCreatedAt())
                    .categorizationConfidence(transaction.getCategorizationConfidence())
                    .categorizationSource(transaction.getCategorizationSource())
                    .categorizationNormalizedText(transaction.getCategorizationNormalizedText())
                    .build();

            log.debug("TransactionResponse created successfully for transaction {}", transaction.getId());
            return response;
        } catch (Exception e) {
            log.error("Error mapping transaction {} to response: {}",
                    transaction != null ? transaction.getId() : "null",
                    e.getMessage(), e);
            throw new RuntimeException("Error mapping transaction: " + e.getMessage(), e);
        }
    }

    private void applyCategorizationMetadata(Transaction transaction, CategorizationResult result) {
        if (result == null) {
            transaction.setCategorizationConfidence(null);
            transaction.setCategorizationSource(null);
            transaction.setCategorizationNormalizedText(null);
            return;
        }

        transaction.setCategorizationConfidence(result.getConfidence());
        transaction.setCategorizationSource(result.getSource());
        transaction.setCategorizationNormalizedText(result.getNormalizedText());
    }

    private CategorizationResult resolveRequestCategorizationMetadata(TransactionRequest request) {
        if (request.getCategory() == null) {
            return null;
        }

        if (request.getCategorizationSource() == null || request.getCategorizationSource().isBlank()) {
            return null;
        }

        String normalizedText = request.getNormalizedText();
        if (normalizedText == null || normalizedText.isBlank()) {
            normalizedText = com.adem.attijari_compass.util.TransactionTextNormalizer.normalize(
                    request.getMerchantName(),
                    request.getDescription()
            );
        }

        return CategorizationResult.builder()
                .category(request.getCategory())
                .confidence(request.getCategorizationConfidence() != null ? request.getCategorizationConfidence() : 0.0d)
                .source(request.getCategorizationSource())
                .normalizedText(normalizedText)
                .build();
    }

    private void recordCorrectionFromRequest(
            Transaction transaction,
            TransactionRequest request,
            TransactionCategory predictedCategory,
            TransactionCategory selectedCategory,
            Long userId
    ) {
        if (predictedCategory != null) {
            transactionCategoryFeedbackService.recordCorrection(
                    transaction.getId(),
                    predictedCategory,
                    selectedCategory,
                    request.getNormalizedText(),
                    request.getCategorizationConfidence(),
                    request.getCategorizationSource(),
                    userId,
                    request.getMerchantName(),
                    request.getDescription()
            );
            return;
        }

        transactionCategoryFeedbackService.recordCorrection(transaction, selectedCategory, userId);
    }

    private PaymentMethod resolvePaymentMethod(TransactionRequest request) {
        return request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.CARD;
    }

    private UserCard resolveUserCard(Long userCardId, PaymentMethod paymentMethod, User user) {
        if (paymentMethod != PaymentMethod.CARD || userCardId == null) {
            return null;
        }

        UserCard userCard = userCardRepository.findById(userCardId)
                .orElseThrow(() -> new ResourceNotFoundException("User card not found with id: " + userCardId));

        if (userCard.getUser() == null || !user.getId().equals(userCard.getUser().getId())) {
            throw new IllegalArgumentException("Selected card is not linked to the current user");
        }

        return userCard;
    }
}

package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.transaction.TransactionCashBreakdownItemRequest;
import com.adem.attijari_compass.dto.transaction.TransactionCashBreakdownItemResponse;
import com.adem.attijari_compass.dto.transaction.TransactionCashBreakdownRequest;
import com.adem.attijari_compass.dto.transaction.TransactionCashBreakdownResponse;
import com.adem.attijari_compass.entity.PaymentMethod;
import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.TransactionCashBreakdown;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.repository.TransactionCashBreakdownRepository;
import com.adem.attijari_compass.repository.TransactionRepository;
import com.adem.attijari_compass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionCashBreakdownService {

    private static final double EPSILON = 0.01d;

    private final TransactionRepository transactionRepository;
    private final TransactionCashBreakdownRepository transactionCashBreakdownRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public TransactionCashBreakdownResponse getBreakdown(Long transactionId, String email) {
        Transaction transaction = getOwnedTransaction(transactionId, email);
        List<TransactionCashBreakdown> items = transactionCashBreakdownRepository.findAllByTransaction_IdOrderByIdAsc(transactionId);
        return buildResponse(transaction, items);
    }

    public TransactionCashBreakdownResponse saveBreakdown(
            Long transactionId,
            TransactionCashBreakdownRequest request,
            String email
    ) {
        Transaction transaction = getOwnedTransaction(transactionId, email);
        validateCashBreakdownTarget(transaction);

        List<TransactionCashBreakdownItemRequest> requestedItems = request.items().stream()
                .filter(item -> item != null && item.amount() != null && item.amount() > 0)
                .toList();

        if (requestedItems.isEmpty()) {
            throw new IllegalArgumentException("At least one cash breakdown line is required.");
        }

        double transactionAmount = Math.abs(transaction.getAmount() != null ? transaction.getAmount() : 0d);
        double allocatedAmount = requestedItems.stream()
                .map(TransactionCashBreakdownItemRequest::amount)
                .mapToDouble(Double::doubleValue)
                .sum();

        if (allocatedAmount - transactionAmount > EPSILON) {
            throw new IllegalArgumentException(
                    "The cash breakdown total cannot exceed the original transaction amount."
            );
        }

        transactionCashBreakdownRepository.deleteAllByTransactionId(transactionId);

        List<TransactionCashBreakdown> savedItems = requestedItems.stream()
                .map(item -> transactionCashBreakdownRepository.save(
                        TransactionCashBreakdown.builder()
                                .transaction(transaction)
                                .category(item.category())
                                .amount(roundMoney(item.amount()))
                                .note(normalizeOptional(item.note()))
                                .build()
                ))
                .toList();

        return buildResponse(transaction, savedItems);
    }

    private Transaction getOwnedTransaction(Long transactionId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return transactionRepository.findByIdAndUserId(transactionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));
    }

    private void validateCashBreakdownTarget(Transaction transaction) {
        if (transaction.getPaymentMethod() != PaymentMethod.CASH) {
            throw new IllegalArgumentException("Cash breakdown is only available for cash transactions.");
        }

        if (transaction.getType() != TransactionType.DEPENSE) {
            throw new IllegalArgumentException("Cash breakdown is only available for cash expenses.");
        }
    }

    private TransactionCashBreakdownResponse buildResponse(
            Transaction transaction,
            List<TransactionCashBreakdown> items
    ) {
        double transactionAmount = Math.abs(transaction.getAmount() != null ? transaction.getAmount() : 0d);
        double allocatedAmount = items.stream()
                .map(TransactionCashBreakdown::getAmount)
                .filter(amount -> amount != null && Double.isFinite(amount))
                .mapToDouble(Double::doubleValue)
                .sum();
        double remainingAmount = Math.max(0d, roundMoney(transactionAmount - allocatedAmount));
        boolean complete = Math.abs(transactionAmount - allocatedAmount) <= EPSILON;

        return TransactionCashBreakdownResponse.builder()
                .transactionId(transaction.getId())
                .transactionAmount(roundMoney(transactionAmount))
                .allocatedAmount(roundMoney(allocatedAmount))
                .remainingAmount(remainingAmount)
                .complete(complete)
                .items(items.stream().map(this::mapItem).toList())
                .build();
    }

    private TransactionCashBreakdownItemResponse mapItem(TransactionCashBreakdown item) {
        TransactionCategory category = item.getCategory();

        return TransactionCashBreakdownItemResponse.builder()
                .id(item.getId())
                .category(category != null ? category.name() : TransactionCategory.fallback().name())
                .categoryLabel(humanizeCategory(category))
                .amount(roundMoney(item.getAmount()))
                .note(item.getNote())
                .build();
    }

    private String humanizeCategory(TransactionCategory category) {
        if (category == null) {
            return TransactionCategory.fallback().label();
        }
        return category.label();
    }

    private String normalizeOptional(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    private double roundMoney(Double value) {
        double safe = value == null || !Double.isFinite(value) ? 0d : value;
        return Math.round(safe * 100d) / 100d;
    }
}

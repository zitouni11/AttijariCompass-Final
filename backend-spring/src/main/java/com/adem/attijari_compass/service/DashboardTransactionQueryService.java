package com.adem.attijari_compass.service;

import com.adem.attijari_compass.entity.CardTransaction;
import com.adem.attijari_compass.entity.PaymentMethod;
import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionSource;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.entity.UserCard;
import com.adem.attijari_compass.repository.CardTransactionRepository;
import com.adem.attijari_compass.repository.TransactionRepository;
import com.adem.attijari_compass.util.CardMaskingUtil;
import com.adem.attijari_compass.util.TransactionTextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardTransactionQueryService {

    private final TransactionRepository transactionRepository;
    private final CardTransactionRepository cardTransactionRepository;

    public List<DashboardTransactionSnapshot> loadMonthlyTransactions(Long userId, YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        Map<String, DashboardTransactionSnapshot> uniqueTransactions = new LinkedHashMap<>();

        for (Transaction transaction : transactionRepository.findDashboardTransactionsByUserIdAndDateBetween(userId, startDate, endDate)) {
            DashboardTransactionSnapshot snapshot = toSnapshot(transaction);
            if (snapshot != null) {
                uniqueTransactions.putIfAbsent(snapshot.dedupeKey(), snapshot);
            }
        }

        for (CardTransaction transaction : cardTransactionRepository.findDashboardTransactionsByUserIdAndTransactionDateBetween(
                userId,
                startDateTime,
                endDateTime
        )) {
            DashboardTransactionSnapshot snapshot = toSnapshot(transaction);
            if (snapshot != null) {
                uniqueTransactions.putIfAbsent(snapshot.dedupeKey(), snapshot);
            }
        }

        return new ArrayList<>(uniqueTransactions.values());
    }

    private DashboardTransactionSnapshot toSnapshot(Transaction transaction) {
        if (transaction == null || transaction.getType() == null || transaction.getDate() == null) {
            return null;
        }

        BigDecimal absoluteAmount = toAbsoluteBigDecimal(transaction.getAmount());
        if (absoluteAmount.signum() == 0) {
            return null;
        }

        BigDecimal signedAmount = transaction.getType() == TransactionType.REVENU
                ? absoluteAmount
                : absoluteAmount.negate();

        String merchantLabel = firstNonBlank(transaction.getMerchantName(), transaction.getDescription());
        String dedupeKey = buildTransactionDedupeKey(transaction, signedAmount, merchantLabel);

        return new DashboardTransactionSnapshot(
                transaction.getDate(),
                transaction.getCreatedAt() != null ? transaction.getCreatedAt() : transaction.getDate().atStartOfDay(),
                signedAmount,
                normalizeCategoryLabel(transaction.getCategory() != null ? transaction.getCategory().name() : null),
                merchantLabel,
                dedupeKey
        );
    }

    private DashboardTransactionSnapshot toSnapshot(CardTransaction transaction) {
        if (transaction == null || transaction.getTransactionDate() == null || transaction.getAmount() == null) {
            return null;
        }

        BigDecimal signedAmount = transaction.getAmount().stripTrailingZeros();
        if (signedAmount.signum() == 0) {
            return null;
        }

        String merchantLabel = firstNonBlank(transaction.getMerchantName(), transaction.getDescription());
        String dedupeKey = buildCardTransactionDedupeKey(transaction, signedAmount, merchantLabel);

        return new DashboardTransactionSnapshot(
                transaction.getTransactionDate().toLocalDate(),
                transaction.getTransactionDate(),
                signedAmount,
                normalizeCategoryLabel(transaction.getCategory()),
                merchantLabel,
                dedupeKey
        );
    }

    private String buildTransactionDedupeKey(Transaction transaction, BigDecimal signedAmount, String merchantLabel) {
        String externalReference = normalizeExternalReference(transaction.getExternalReference());
        if (!externalReference.isBlank()) {
            return "EXT|" + externalReference;
        }

        if (!isCardOrigin(transaction)) {
            return "MAIN|" + transaction.getId();
        }

        return buildFallbackCardDedupeKey(
                transaction.getDate(),
                signedAmount,
                merchantLabel,
                resolveCardToken(transaction.getUserCard(), transaction.getCardLast4())
        );
    }

    private String buildCardTransactionDedupeKey(CardTransaction transaction, BigDecimal signedAmount, String merchantLabel) {
        String externalReference = normalizeExternalReference(transaction.getExternalReference());
        if (!externalReference.isBlank()) {
            return "EXT|" + externalReference;
        }

        UserCard userCard = transaction.getUserCard();
        return buildFallbackCardDedupeKey(
                transaction.getTransactionDate().toLocalDate(),
                signedAmount,
                merchantLabel,
                resolveCardToken(userCard, userCard != null ? userCard.getLast4() : null)
        );
    }

    private String buildFallbackCardDedupeKey(
            LocalDate businessDate,
            BigDecimal signedAmount,
            String merchantLabel,
            String cardToken
    ) {
        return "CARD|"
                + cardToken
                + "|"
                + businessDate
                + "|"
                + normalizeAmountKey(signedAmount)
                + "|"
                + TransactionTextNormalizer.normalize(merchantLabel);
    }

    private String resolveCardToken(UserCard userCard, String explicitLast4) {
        String last4 = firstNonBlank(
                explicitLast4,
                userCard != null ? userCard.getLast4() : null,
                userCard != null ? CardMaskingUtil.extractLast4(userCard.getMaskedCardNumber()) : null
        );

        if (!last4.isBlank()) {
            return "L4:" + last4;
        }
        if (userCard != null && userCard.getId() != null) {
            return "UC:" + userCard.getId();
        }
        return "CARD";
    }

    private boolean isCardOrigin(Transaction transaction) {
        return transaction.getUserCard() != null
                || transaction.getPaymentMethod() == PaymentMethod.CARD
                || transaction.getSource() == TransactionSource.MANUAL_CARD
                || transaction.getSource() == TransactionSource.CARD_SYNC
                || transaction.getSource() == TransactionSource.CARD_SANDBOX;
    }

    private BigDecimal toAbsoluteBigDecimal(Double amount) {
        return amount == null ? BigDecimal.ZERO : BigDecimal.valueOf(amount).abs();
    }

    private String normalizeExternalReference(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeAmountKey(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String normalizeCategoryLabel(String category) {
        if (category == null || category.isBlank()) {
            return TransactionCategory.fallback().name();
        }
        return TransactionCategory.fromValue(category).name().replace('_', ' ');
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    public record DashboardTransactionSnapshot(
            LocalDate businessDate,
            LocalDateTime sortTimestamp,
            BigDecimal signedAmount,
            String category,
            String merchantName,
            String dedupeKey
    ) {
        public boolean isIncome() {
            return signedAmount != null && signedAmount.signum() > 0;
        }

        public boolean isExpense() {
            return signedAmount != null && signedAmount.signum() < 0;
        }

        public BigDecimal absoluteAmount() {
            return signedAmount == null ? BigDecimal.ZERO : signedAmount.abs();
        }
    }
}

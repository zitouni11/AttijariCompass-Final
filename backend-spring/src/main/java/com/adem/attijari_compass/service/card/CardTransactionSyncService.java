package com.adem.attijari_compass.service.card;

import com.adem.attijari_compass.entity.CardPool;
import com.adem.attijari_compass.entity.CardPoolTransaction;
import com.adem.attijari_compass.entity.CardTransaction;
import com.adem.attijari_compass.entity.PaymentMethod;
import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionSource;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.entity.UserCard;
import com.adem.attijari_compass.model.categorization.CategorizationResult;
import com.adem.attijari_compass.repository.CardPoolTransactionRepository;
import com.adem.attijari_compass.repository.CardTransactionRepository;
import com.adem.attijari_compass.repository.TransactionRepository;
import com.adem.attijari_compass.service.SmartCategorizationService;
import com.adem.attijari_compass.util.CardMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardTransactionSyncService {

    private static final DateTimeFormatter EXTERNAL_REFERENCE_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final String CATEGORIZATION_SOURCE = "CARD_SYNC_TEMPLATE";

    private final CardPoolTransactionRepository cardPoolTransactionRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final TransactionRepository transactionRepository;
    private final SmartCategorizationService smartCategorizationService;

    @Transactional
    public TransactionSyncResult syncPoolTransactions(UserCard userCard, CardPool cardPool) {
        List<CardPoolTransaction> poolTransactions = cardPoolTransactionRepository.findAllByCardPoolIdOrderByTransactionDateDesc(cardPool.getId());
        if (poolTransactions.isEmpty()) {
            return new TransactionSyncResult(0, 0);
        }

        int cardInserted = 0;
        int cardUpdated = 0;
        int globalInserted = 0;
        int globalUpdated = 0;

        for (CardPoolTransaction poolTransaction : poolTransactions) {
            String externalReference = resolveExternalReference(userCard, poolTransaction);

            CardTransaction cardTransaction = resolveCardTransaction(userCard.getId(), externalReference);
            if (cardTransaction.getId() == null) {
                cardInserted++;
            } else {
                cardUpdated++;
            }
            syncManagedCardTransaction(cardTransaction, userCard, poolTransaction, externalReference);
            cardTransactionRepository.save(cardTransaction);

            Transaction globalTransaction = resolveGlobalTransaction(userCard.getId(), externalReference);
            if (globalTransaction.getId() == null) {
                globalInserted++;
            } else {
                globalUpdated++;
            }
            syncGlobalTransaction(globalTransaction, userCard, poolTransaction, externalReference);
            transactionRepository.save(globalTransaction);
        }

        log.info(
                "Synchronized {} pool transactions for userCard {}: card_transaction inserted={}, updated={}, transaction inserted={}, updated={}",
                poolTransactions.size(),
                userCard.getId(),
                cardInserted,
                cardUpdated,
                globalInserted,
                globalUpdated
        );

        return new TransactionSyncResult(cardInserted, cardUpdated);
    }

    private CardTransaction resolveCardTransaction(Long userCardId, String externalReference) {
        if (!StringUtils.hasText(externalReference)) {
            return new CardTransaction();
        }
        return cardTransactionRepository.findByUserCardIdAndExternalReference(userCardId, externalReference)
                .orElseGet(CardTransaction::new);
    }

    private Transaction resolveGlobalTransaction(Long userCardId, String externalReference) {
        if (!StringUtils.hasText(externalReference)) {
            return new Transaction();
        }
        return transactionRepository.findByUserCardIdAndExternalReference(userCardId, externalReference)
                .orElseGet(Transaction::new);
    }

    private void syncManagedCardTransaction(
            CardTransaction transaction,
            UserCard userCard,
            CardPoolTransaction poolTransaction,
            String externalReference
    ) {
        transaction.setUserCard(userCard);
        transaction.setTransactionDate(poolTransaction.getTransactionDate());
        transaction.setValueDate(poolTransaction.getValueDate());
        transaction.setAmount(poolTransaction.getAmount());
        transaction.setMerchantName(poolTransaction.getMerchantName());
        transaction.setDescription(poolTransaction.getDescription());
        transaction.setReference(poolTransaction.getReference());
        transaction.setCategory(poolTransaction.getCategory());
        transaction.setStatus(poolTransaction.getStatus());
        transaction.setCity(poolTransaction.getCity());
        transaction.setCountry(poolTransaction.getCountry());
        transaction.setCurrency("TND");
        transaction.setInstallment(poolTransaction.isInstallment());
        transaction.setInstallmentIndex(poolTransaction.getInstallmentIndex());
        transaction.setInstallmentTotal(poolTransaction.getInstallmentTotal());
        transaction.setExternalReference(externalReference);
    }

    private void syncGlobalTransaction(
            Transaction transaction,
            UserCard userCard,
            CardPoolTransaction poolTransaction,
            String externalReference
    ) {
        transaction.setUser(userCard.getUser());
        transaction.setUserCard(userCard);
        transaction.setDescription(resolveDescription(poolTransaction));
        transaction.setAmount(resolveGlobalAmount(poolTransaction));
        transaction.setDate(poolTransaction.getTransactionDate().toLocalDate());
        transaction.setType(resolveTransactionType(poolTransaction));
        transaction.setMerchantName(resolveMerchantName(poolTransaction));
        transaction.setPaymentMethod(PaymentMethod.CARD);
        transaction.setSource(TransactionSource.CARD_SYNC);
        transaction.setCardLast4(resolveCardLast4(userCard));
        transaction.setExternalReference(externalReference);

        if (transaction.getCreatedAt() == null) {
            transaction.setCreatedAt(LocalDateTime.now());
        }

        if (transaction.getId() == null || transaction.getCategory() == null) {
            AppliedCategorization categorization = resolveCategorization(poolTransaction);
            transaction.setCategory(categorization.category());
            transaction.setCategorizationConfidence(categorization.confidence());
            transaction.setCategorizationSource(categorization.source());
            transaction.setCategorizationNormalizedText(categorization.normalizedText());
        }
    }

    private AppliedCategorization resolveCategorization(CardPoolTransaction poolTransaction) {
        String categorizationText = buildCategorizationText(poolTransaction);
        TransactionCategory mappedCategory = mapTemplateCategory(categorizationText);
        if (mappedCategory != null) {
            return new AppliedCategorization(mappedCategory, 1.0d, CATEGORIZATION_SOURCE, categorizationText);
        }

        CategorizationResult result = smartCategorizationService.categorize(
                resolveMerchantName(poolTransaction),
                categorizationText
        );

        return new AppliedCategorization(
                result.getCategory() != null ? result.getCategory() : TransactionCategory.fallback(),
                result.getConfidence(),
                result.getSource(),
                result.getNormalizedText()
        );
    }

    private TransactionCategory mapTemplateCategory(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return null;
        }
        if (containsAny(normalized, "salaire", "salary", "payroll", "wage")) {
            return TransactionCategory.SALAIRE;
        }
        if (containsAny(normalized, "epargne", "saving", "savings", "investment", "investissement")) {
            return TransactionCategory.EPARGNE;
        }
        if (containsAny(normalized, "virement", "cashback", "versement", "allocation", "remboursement", "pension", "freelance", "revenus")) {
            return TransactionCategory.BANQUE;
        }
        if (containsAny(normalized, "restaurant", "gastronomie", "diner", "restauration", "fastfood", "fast food")) {
            return TransactionCategory.RESTAURANT;
        }
        if (containsAny(normalized, "cafe", "coffee", "espresso")) {
            return TransactionCategory.CAFES;
        }
        if (containsAny(normalized, "livraison", "delivery", "glovo", "talabat", "foody")) {
            return TransactionCategory.LIVRAISON;
        }
        if (containsAny(normalized, "carrefour", "monoprix", "mg", "grande distribution", "courses")) {
            return TransactionCategory.SUPERMARCHE;
        }
        if (containsAny(normalized, "carburant", "shell", "total", "station service", "station-service", "agil", "ola energy")) {
            return TransactionCategory.STATION_SERVICES;
        }
        if (containsAny(normalized, "garage", "pneu", "vidange", "mecanique", "service auto")) {
            return TransactionCategory.SERVICE_AUTO;
        }
        if (containsAny(normalized, "uber", "bolt", "transport", "aeroport", "taxi")) {
            return TransactionCategory.TRANSPORT;
        }
        if (containsAny(normalized, "pharmacie", "sante", "clinique")) {
            return TransactionCategory.SANTE;
        }
        if (containsAny(normalized, "beauty", "barber", "coiffeur", "spa", "cosmetic")) {
            return TransactionCategory.BEAUTE;
        }
        if (containsAny(normalized, "shopping", "mode", "zara", "fnac", "lc waikiki", "e-commerce")) {
            return TransactionCategory.SHOPPING;
        }
        if (containsAny(normalized, "tech", "github", "openai", "azure", "aws", "figma", "adobe", "cloud")) {
            return TransactionCategory.TECHNOLOGIE;
        }
        if (containsAny(normalized, "telecom", "orange", "ooredoo", "tunisie telecom")) {
            return TransactionCategory.OPERATEURS_TELEPHONIQUES;
        }
        if (containsAny(normalized, "facture", "factures", "bill", "invoice")) {
            return TransactionCategory.FACTURES;
        }
        if (containsAny(normalized, "steg", "sonede", "electricite", "eau")) {
            return TransactionCategory.STEG_SONEDE;
        }
        if (containsAny(normalized, "abonnement", "services digitaux", "developpement", "productivite", "design")) {
            return TransactionCategory.TECHNOLOGIE;
        }
        if (containsAny(normalized, "loyer", "rent", "logement", "housing", "apartment", "residence", "maison", "home")) {
            return TransactionCategory.LOGEMENT;
        }
        if (containsAny(normalized, "hotel", "hotellerie", "booking", "airbnb")) {
            return TransactionCategory.HOTEL;
        }
        if (containsAny(normalized, "voyage", "tunisair", "air france", "travel")) {
            return TransactionCategory.VOYAGE;
        }
        if (containsAny(normalized, "cinema", "concert", "event", "festival")) {
            return TransactionCategory.DIVERTISSEMENT;
        }
        if (containsAny(normalized, "education", "formation")) {
            return TransactionCategory.EDUCATION;
        }
        if (containsAny(normalized, "frais bancaire")) {
            return TransactionCategory.BANQUE;
        }
        return null;
    }

    private String buildCategorizationText(CardPoolTransaction poolTransaction) {
        StringBuilder builder = new StringBuilder();
        appendToken(builder, poolTransaction.getCategory());
        appendToken(builder, poolTransaction.getMerchantName());
        appendToken(builder, poolTransaction.getDescription());
        appendToken(builder, poolTransaction.getReference());
        return builder.toString().trim();
    }

    private TransactionType resolveTransactionType(CardPoolTransaction poolTransaction) {
        return poolTransaction.getAmount().signum() > 0
                ? TransactionType.REVENU
                : TransactionType.DEPENSE;
    }

    private Double resolveGlobalAmount(CardPoolTransaction poolTransaction) {
        return poolTransaction.getAmount().abs().doubleValue();
    }

    private String resolveDescription(CardPoolTransaction poolTransaction) {
        return firstNonBlank(
                poolTransaction.getDescription(),
                poolTransaction.getMerchantName(),
                poolTransaction.getReference(),
                "Transaction carte synchronisee"
        );
    }

    private String resolveMerchantName(CardPoolTransaction poolTransaction) {
        return firstNonBlank(
                poolTransaction.getMerchantName(),
                poolTransaction.getDescription(),
                "Transaction carte"
        );
    }

    private String resolveCardLast4(UserCard userCard) {
        return firstNonBlank(
                userCard.getLast4(),
                CardMaskingUtil.extractLast4(userCard.getMaskedCardNumber()),
                CardMaskingUtil.extractLast4(userCard.getCardNumber())
        );
    }

    private String resolveExternalReference(UserCard userCard, CardPoolTransaction poolTransaction) {
        if (StringUtils.hasText(poolTransaction.getExternalReference())) {
            return poolTransaction.getExternalReference().trim();
        }

        String referenceToken = normalizeToken(firstNonBlank(
                poolTransaction.getReference(),
                poolTransaction.getMerchantName(),
                poolTransaction.getDescription(),
                "TX"
        ));
        String timestampToken = poolTransaction.getTransactionDate() == null
                ? "000000000000"
                : poolTransaction.getTransactionDate().format(EXTERNAL_REFERENCE_TIMESTAMP_FORMAT);
        String amountToken = poolTransaction.getAmount() == null
                ? "0"
                : poolTransaction.getAmount().abs().setScale(2, RoundingMode.HALF_UP).toPlainString().replace(".", "");

        return "CARDSYNC-" + userCard.getId() + "-" + referenceToken + "-" + timestampToken + "-" + amountToken;
    }

    private String normalizeToken(String value) {
        String normalized = value == null
                ? "TX"
                : value.trim().replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "TX";
        }
        return normalized.length() > 20 ? normalized.substring(0, 20) : normalized;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean containsAny(String value, String... tokens) {
        if (value == null || tokens == null) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private void appendToken(StringBuilder builder, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(value.trim());
    }

    public record TransactionSyncResult(int inserted, int updated) {
    }

    private record AppliedCategorization(
            TransactionCategory category,
            Double confidence,
            String source,
            String normalizedText
    ) {
    }
}

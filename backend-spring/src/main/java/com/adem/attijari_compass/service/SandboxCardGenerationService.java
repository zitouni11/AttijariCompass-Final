package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.card.GenerateTestCardRequest;
import com.adem.attijari_compass.dto.card.GenerateTestCardResponse;
import com.adem.attijari_compass.dto.card.GeneratedTestCardResponse;
import com.adem.attijari_compass.entity.CardStatus;
import com.adem.attijari_compass.entity.CardType;
import com.adem.attijari_compass.entity.SandboxCardProfile;
import com.adem.attijari_compass.entity.SandboxTransactionType;
import com.adem.attijari_compass.entity.TestCardCatalog;
import com.adem.attijari_compass.entity.TestCardTransaction;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.exception.AuthenticationRequiredException;
import com.adem.attijari_compass.repository.TestCardCatalogRepository;
import com.adem.attijari_compass.repository.TestCardTransactionRepository;
import com.adem.attijari_compass.util.CardMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SandboxCardGenerationService {

    private static final String GENERATED_REFERENCE_PREFIX = "GEN";
    private static final List<String> STUDENT_BANKS = List.of("Attijari Bank Tunisie", "UIB", "Amen Bank");
    private static final List<String> SALARIED_BANKS = List.of("Attijari Bank Tunisie", "BIAT", "Banque de Tunisie", "UIB");
    private static final List<String> FAMILY_BANKS = List.of("Attijari Bank Tunisie", "BIAT", "BH Bank");
    private static final List<String> PREMIUM_BANKS = List.of("Attijari Bank Tunisie", "Banque de Tunisie", "Amen Bank");

    private final TestCardCatalogRepository testCardCatalogRepository;
    private final TestCardTransactionRepository testCardTransactionRepository;
    private final CardService cardService;

    public GenerateTestCardResponse generateSandboxCard(GenerateTestCardRequest request, String currentUserEmail) {
        if (request.isConnectToCurrentUser() && (currentUserEmail == null || currentUserEmail.isBlank())) {
            throw new AuthenticationRequiredException(
                    "Authentication is required to connect the generated card to the current user"
            );
        }

        CardType cardType = chooseCardType(request.getProfile());
        String testCardNumber = generateUniqueCardNumber(cardType);
        TestCardCatalog card = buildSandboxCard(request.getHolderName(), request.getProfile(), cardType, testCardNumber);
        TestCardCatalog savedCard = testCardCatalogRepository.save(card);

        List<TestCardTransaction> generatedTransactions = generateTransactions(
                savedCard,
                request.getProfile(),
                request.getTransactionCount()
        );
        testCardTransactionRepository.saveAll(generatedTransactions);

        GenerateTestCardResponse.GenerateTestCardResponseBuilder responseBuilder = GenerateTestCardResponse.builder()
                .message(request.isConnectToCurrentUser()
                        ? "Sandbox card generated and connected successfully"
                        : "Sandbox card generated successfully")
                .profile(request.getProfile())
                .card(mapGeneratedCard(savedCard))
                .generatedTransactions(generatedTransactions.size())
                .importedTransactions(0)
                .connectedToCurrentUser(request.isConnectToCurrentUser());

        if (request.isConnectToCurrentUser()) {
            var syncResponse = cardService.connectGeneratedCard(savedCard, currentUserEmail);
            responseBuilder
                    .importedTransactions(syncResponse.getImportedTransactions())
                    .connectedCard(syncResponse.getCard())
                    .syncedAt(syncResponse.getSyncedAt());
        }

        log.info(
                "Generated sandbox card {} for profile {} with {} transactions",
                savedCard.getMaskedCardNumber(),
                request.getProfile(),
                generatedTransactions.size()
        );
        return responseBuilder.build();
    }

    private TestCardCatalog buildSandboxCard(
            String holderName,
            SandboxCardProfile profile,
            CardType cardType,
            String testCardNumber
    ) {
        YearMonth expiryDate = YearMonth.now().plusMonths(ThreadLocalRandom.current().nextInt(24, 61));

        return TestCardCatalog.builder()
                .holderName(normalizeHolderName(holderName))
                .maskedCardNumber(CardMaskingUtil.maskCardNumber(testCardNumber))
                .testCardNumber(testCardNumber)
                .expiryMonth(expiryDate.getMonthValue())
                .expiryYear(expiryDate.getYear())
                .cvv(String.format("%03d", ThreadLocalRandom.current().nextInt(100, 1000)))
                .cardType(cardType)
                .bankName(selectBankName(profile))
                .status(CardStatus.ACTIVE)
                .initialBalance(generateInitialBalance(profile))
                .build();
    }

    private List<TestCardTransaction> generateTransactions(
            TestCardCatalog card,
            SandboxCardProfile profile,
            int transactionCount
    ) {
        ProfileScenario scenario = buildScenario(profile);
        List<TestCardTransaction> transactions = new ArrayList<>();
        String referencePrefix = GENERATED_REFERENCE_PREFIX + "-" + profile.name() + "-" + card.getId() + "-" + System.currentTimeMillis();
        int sequence = 1;

        for (TransactionTemplate template : scenario.recurringTemplates()) {
            for (int occurrence = 0; occurrence < template.occurrences() && transactions.size() < transactionCount; occurrence++) {
                transactions.add(buildTransaction(card, template, referencePrefix, sequence++, occurrence));
            }
        }

        for (TransactionTemplate template : scenario.variableTemplates()) {
            if (transactions.size() >= transactionCount) {
                break;
            }
            transactions.add(buildTransaction(card, template, referencePrefix, sequence++, transactions.size()));
        }

        while (transactions.size() < transactionCount) {
            TransactionTemplate template = randomItem(scenario.variableTemplates());
            transactions.add(buildTransaction(card, template, referencePrefix, sequence++, transactions.size()));
        }

        transactions.sort(Comparator.comparing(TestCardTransaction::getTransactionDate).reversed());
        return transactions;
    }

    private TestCardTransaction buildTransaction(
            TestCardCatalog card,
            TransactionTemplate template,
            String referencePrefix,
            int sequence,
            int occurrenceIndex
    ) {
        int offsetDays = resolveOffsetDays(template, occurrenceIndex);
        int hour = randomBetween(template.minHour(), template.maxHour());
        int minute = ThreadLocalRandom.current().nextInt(0, 60);
        double amount = roundAmount(randomAmount(template.minAmount(), template.maxAmount()));

        return TestCardTransaction.builder()
                .testCard(card)
                .merchantName(template.merchantName())
                .rawLabel(template.rawLabel())
                .amount(amount)
                .transactionType(template.transactionType())
                .transactionDate(LocalDateTime.now()
                        .minusDays(offsetDays)
                        .withHour(hour)
                        .withMinute(minute)
                        .withSecond(0)
                        .withNano(0))
                .categorySuggestion(template.category())
                .description(template.description())
                .externalReference(referencePrefix + "-" + String.format("%03d", sequence))
                .build();
    }

    private int resolveOffsetDays(TransactionTemplate template, int occurrenceIndex) {
        int jitter = ThreadLocalRandom.current().nextInt(0, 3);
        return switch (template.cadence()) {
            case MONTHLY -> Math.min(90, template.baseOffsetDays() + (occurrenceIndex * 28) + jitter);
            case BIWEEKLY -> Math.min(90, template.baseOffsetDays() + (occurrenceIndex * 14) + jitter);
            case WEEKLY -> Math.min(90, template.baseOffsetDays() + (occurrenceIndex * 7) + jitter);
            case OCCASIONAL -> randomBetween(1, 90);
        };
    }

    private String generateUniqueCardNumber(CardType cardType) {
        for (int attempt = 0; attempt < 200; attempt++) {
            String prefix = cardType == CardType.VISA
                    ? "4" + ThreadLocalRandom.current().nextInt(10000, 99999)
                    : "5" + ThreadLocalRandom.current().nextInt(10000, 99999);
            StringBuilder partial = new StringBuilder(prefix);
            while (partial.length() < 15) {
                partial.append(ThreadLocalRandom.current().nextInt(0, 10));
            }

            String candidate = partial + String.valueOf(computeLuhnCheckDigit(partial.toString()));
            if (!testCardCatalogRepository.existsByTestCardNumber(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException("Unable to generate a unique sandbox card number");
    }

    private int computeLuhnCheckDigit(String partialNumber) {
        int sum = 0;
        boolean shouldDouble = true;

        for (int index = partialNumber.length() - 1; index >= 0; index--) {
            int digit = partialNumber.charAt(index) - '0';
            if (shouldDouble) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            shouldDouble = !shouldDouble;
        }

        return (10 - (sum % 10)) % 10;
    }

    private CardType chooseCardType(SandboxCardProfile profile) {
        return switch (profile) {
            case STUDENT, SALARIED -> ThreadLocalRandom.current().nextBoolean() ? CardType.VISA : CardType.MASTERCARD;
            case FAMILY -> ThreadLocalRandom.current().nextInt(100) < 65 ? CardType.MASTERCARD : CardType.VISA;
            case PREMIUM -> ThreadLocalRandom.current().nextInt(100) < 70 ? CardType.MASTERCARD : CardType.VISA;
        };
    }

    private String selectBankName(SandboxCardProfile profile) {
        return switch (profile) {
            case STUDENT -> randomItem(STUDENT_BANKS);
            case SALARIED -> randomItem(SALARIED_BANKS);
            case FAMILY -> randomItem(FAMILY_BANKS);
            case PREMIUM -> randomItem(PREMIUM_BANKS);
        };
    }

    private double generateInitialBalance(SandboxCardProfile profile) {
        return roundAmount(switch (profile) {
            case STUDENT -> randomAmount(250.0, 1_800.0);
            case SALARIED -> randomAmount(4_000.0, 14_500.0);
            case FAMILY -> randomAmount(9_500.0, 26_000.0);
            case PREMIUM -> randomAmount(30_000.0, 92_000.0);
        });
    }

    private GeneratedTestCardResponse mapGeneratedCard(TestCardCatalog card) {
        return GeneratedTestCardResponse.builder()
                .id(card.getId())
                .holderName(card.getHolderName())
                .maskedCardNumber(card.getMaskedCardNumber())
                .testCardNumber(card.getTestCardNumber())
                .expiryMonth(card.getExpiryMonth())
                .expiryYear(card.getExpiryYear())
                .cvv(card.getCvv())
                .cardType(card.getCardType())
                .bankName(card.getBankName())
                .status(card.getStatus())
                .initialBalance(card.getInitialBalance())
                .build();
    }

    private ProfileScenario buildScenario(SandboxCardProfile profile) {
        return switch (profile) {
            case STUDENT -> new ProfileScenario(
                    List.of(
                            monthly("Bourse Etudiante", "BOURSE UNIVERSITAIRE TN", 320.0, 780.0, SandboxTransactionType.CREDIT, TransactionCategory.EDUCATION, "Bourse etudiant ou argent recu", 2, 8, 10),
                            monthly("Netflix", "NETFLIX.COM AMSTERDAM", 23.0, 35.0, SandboxTransactionType.DEBIT, TransactionCategory.TECHNOLOGIE, "Abonnement streaming", 2, 5, 7),
                            monthly("Ooredoo", "OOREDOO APP RECHARGE", 10.0, 18.0, SandboxTransactionType.DEBIT, TransactionCategory.OPERATEURS_TELEPHONIQUES, "Recharge forfait mobile", 2, 8, 10),
                            weekly("Uber", "UBER TRIP TUNIS", 7.0, 18.0, SandboxTransactionType.DEBIT, TransactionCategory.TRANSPORT, "Trajet Uber", 4, 7, 22),
                            weekly("Foody", "FOODY STUDENT DELIVERY", 12.0, 28.0, SandboxTransactionType.DEBIT, TransactionCategory.LIVRAISON, "Commande fast food ou repas rapide", 4, 11, 21),
                            weekly("Monoprix", "MONOPRIX CAMPUS TN", 14.0, 42.0, SandboxTransactionType.DEBIT, TransactionCategory.SUPERMARCHE, "Courses du quotidien", 4, 16, 20)
                    ),
                    List.of(
                            occasional("Bolt", "BOLT RIDE TUNIS", 5.0, 15.0, SandboxTransactionType.DEBIT, TransactionCategory.TRANSPORT, "Trajet Bolt", 7, 23),
                            occasional("Carrefour City", "CARREFOUR CITY TN", 8.0, 32.0, SandboxTransactionType.DEBIT, TransactionCategory.SUPERMARCHE, "Petites courses", 16, 21),
                            occasional("KFC Tunis", "KFC TUNIS FASTFOOD", 16.0, 34.0, SandboxTransactionType.DEBIT, TransactionCategory.RESTAURANT, "Repas fast food", 12, 22),
                            occasional("Cinema", "PATHE TUNIS CINEMA", 12.0, 28.0, SandboxTransactionType.DEBIT, TransactionCategory.DIVERTISSEMENT, "Sortie cinema", 18, 22)
                    )
            );
            case SALARIED -> new ProfileScenario(
                    List.of(
                            monthly("Salaire", "PAYROLL TECH COMPANY TN", 2_900.0, 4_800.0, SandboxTransactionType.CREDIT, TransactionCategory.SALAIRE, "Salaire mensuel", 3, 8, 10),
                            monthly("Loyer", "LOYER APPARTEMENT TUNIS", 750.0, 1_350.0, SandboxTransactionType.DEBIT, TransactionCategory.LOGEMENT, "Paiement du loyer", 3, 7, 9),
                            monthly("Orange", "ORANGE MOBILE TUNISIE", 28.0, 72.0, SandboxTransactionType.DEBIT, TransactionCategory.OPERATEURS_TELEPHONIQUES, "Facture mobile ou internet", 3, 9, 11),
                            monthly("Netflix", "NETFLIX.COM SUBSCRIPTION", 23.0, 45.0, SandboxTransactionType.DEBIT, TransactionCategory.TECHNOLOGIE, "Abonnement streaming", 3, 5, 7),
                            weekly("Carrefour", "CARREFOUR MARKET TUNIS", 65.0, 185.0, SandboxTransactionType.DEBIT, TransactionCategory.SUPERMARCHE, "Courses Carrefour", 4, 17, 20),
                            weekly("Uber", "UBER BUSINESS TUNIS", 10.0, 26.0, SandboxTransactionType.DEBIT, TransactionCategory.TRANSPORT, "Trajet Uber domicile travail", 4, 7, 21)
                    ),
                    List.of(
                            occasional("Monoprix", "MONOPRIX URBAIN NORD", 22.0, 85.0, SandboxTransactionType.DEBIT, TransactionCategory.SUPERMARCHE, "Courses Monoprix", 17, 21),
                            occasional("Zara", "ZARA TUNIS CITY", 85.0, 260.0, SandboxTransactionType.DEBIT, TransactionCategory.SHOPPING, "Achat shopping", 18, 21),
                            occasional("Foody", "FOODY OFFICE LUNCH", 18.0, 46.0, SandboxTransactionType.DEBIT, TransactionCategory.LIVRAISON, "Repas au bureau", 12, 14),
                            occasional("Pharmacie", "PHARMACIE LAFAYETTE TN", 14.0, 68.0, SandboxTransactionType.DEBIT, TransactionCategory.SANTE, "Achats pharmacie", 10, 20)
                    )
            );
            case FAMILY -> new ProfileScenario(
                    List.of(
                            monthly("Salaire Foyer", "PAYROLL FAMILY HOUSEHOLD", 4_800.0, 7_500.0, SandboxTransactionType.CREDIT, TransactionCategory.SALAIRE, "Salaire principal du foyer", 3, 8, 10),
                            monthly("Loyer", "LOYER RESIDENCE FAMILIALE", 1_150.0, 2_200.0, SandboxTransactionType.DEBIT, TransactionCategory.LOGEMENT, "Paiement du loyer familial", 3, 7, 9),
                            monthly("Frais Scolaires", "ECOLE PRIVEE TUNIS", 180.0, 520.0, SandboxTransactionType.DEBIT, TransactionCategory.EDUCATION, "Paiement frais scolaires", 3, 8, 11),
                            monthly("STEG", "STEG FACTURE ELECTRICITE", 90.0, 240.0, SandboxTransactionType.DEBIT, TransactionCategory.STEG_SONEDE, "Facture electricite et eau", 3, 9, 11),
                            weekly("Carrefour", "CARREFOUR GRANDES COURSES", 180.0, 420.0, SandboxTransactionType.DEBIT, TransactionCategory.SUPERMARCHE, "Grandes courses familiales", 4, 17, 20),
                            biweekly("Pharmacie", "PHARMACIE FAMILIALE TN", 35.0, 120.0, SandboxTransactionType.DEBIT, TransactionCategory.SANTE, "Achats pharmacie famille", 3, 10, 19)
                    ),
                    List.of(
                            occasional("Monoprix", "MONOPRIX FAMILLE TN", 45.0, 130.0, SandboxTransactionType.DEBIT, TransactionCategory.SUPERMARCHE, "Courses d appoint", 16, 21),
                            occasional("Maison", "BRICOLAGE ET MAISON TN", 70.0, 260.0, SandboxTransactionType.DEBIT, TransactionCategory.LOGEMENT, "Depenses maison", 11, 19),
                            occasional("MG", "MG SUPERETTE FAMILY", 40.0, 140.0, SandboxTransactionType.DEBIT, TransactionCategory.SUPERMARCHE, "Courses MG", 16, 20),
                            occasional("Transport", "UBER FAMILY TRIP TN", 14.0, 42.0, SandboxTransactionType.DEBIT, TransactionCategory.TRANSPORT, "Transport familial", 7, 21)
                    )
            );
            case PREMIUM -> new ProfileScenario(
                    List.of(
                            monthly("Salaire Premium", "PAYROLL EXECUTIVE PREMIUM", 8_500.0, 14_500.0, SandboxTransactionType.CREDIT, TransactionCategory.SALAIRE, "Salaire premium", 3, 8, 10),
                            monthly("Concierge", "PREMIUM CONCIERGE SERVICE", 180.0, 420.0, SandboxTransactionType.DEBIT, TransactionCategory.TECHNOLOGIE, "Service premium mensuel", 3, 9, 11),
                            monthly("Club Prive", "PRIVATE CLUB MEMBERSHIP", 120.0, 360.0, SandboxTransactionType.DEBIT, TransactionCategory.DIVERTISSEMENT, "Cotisation club prive", 3, 8, 10),
                            biweekly("Restaurant Premium", "RESTAURANT GOURMET TUNIS", 180.0, 520.0, SandboxTransactionType.DEBIT, TransactionCategory.RESTAURANT, "Diner premium", 4, 19, 22),
                            biweekly("Uber Black", "UBER BLACK TUNIS", 28.0, 90.0, SandboxTransactionType.DEBIT, TransactionCategory.TRANSPORT, "Trajet premium", 4, 8, 23),
                            biweekly("Voyage", "AIR TUNIS PREMIUM TRAVEL", 350.0, 1_850.0, SandboxTransactionType.DEBIT, TransactionCategory.VOYAGE, "Voyage ou billet premium", 3, 9, 18)
                    ),
                    List.of(
                            occasional("Hotel", "HOTEL RESORT PREMIUM", 420.0, 2_100.0, SandboxTransactionType.DEBIT, TransactionCategory.HOTEL, "Sejour hotel", 11, 22),
                            occasional("Luxury Shopping", "LUXURY SHOPPING AVENUE", 260.0, 1_650.0, SandboxTransactionType.DEBIT, TransactionCategory.SHOPPING, "Shopping premium", 13, 21),
                            occasional("Spa", "SPA WELLNESS PREMIUM", 90.0, 380.0, SandboxTransactionType.DEBIT, TransactionCategory.SANTE, "Spa et bien etre", 10, 20),
                            occasional("Gourmet Store", "GOURMET STORE PREMIUM", 110.0, 320.0, SandboxTransactionType.DEBIT, TransactionCategory.SUPERMARCHE, "Courses haut de gamme", 15, 20)
                    )
            );
        };
    }

    private TransactionTemplate monthly(
            String merchantName,
            String rawLabel,
            double minAmount,
            double maxAmount,
            SandboxTransactionType transactionType,
            TransactionCategory category,
            String description,
            int occurrences,
            int minHour,
            int maxHour
    ) {
        return new TransactionTemplate(
                merchantName, rawLabel, minAmount, maxAmount, transactionType, category,
                description, Cadence.MONTHLY, occurrences, minHour, maxHour, 2
        );
    }

    private TransactionTemplate biweekly(
            String merchantName,
            String rawLabel,
            double minAmount,
            double maxAmount,
            SandboxTransactionType transactionType,
            TransactionCategory category,
            String description,
            int occurrences,
            int minHour,
            int maxHour
    ) {
        return new TransactionTemplate(
                merchantName, rawLabel, minAmount, maxAmount, transactionType, category,
                description, Cadence.BIWEEKLY, occurrences, minHour, maxHour, 3
        );
    }

    private TransactionTemplate weekly(
            String merchantName,
            String rawLabel,
            double minAmount,
            double maxAmount,
            SandboxTransactionType transactionType,
            TransactionCategory category,
            String description,
            int occurrences,
            int minHour,
            int maxHour
    ) {
        return new TransactionTemplate(
                merchantName, rawLabel, minAmount, maxAmount, transactionType, category,
                description, Cadence.WEEKLY, occurrences, minHour, maxHour, 1
        );
    }

    private TransactionTemplate occasional(
            String merchantName,
            String rawLabel,
            double minAmount,
            double maxAmount,
            SandboxTransactionType transactionType,
            TransactionCategory category,
            String description,
            int minHour,
            int maxHour
    ) {
        return new TransactionTemplate(
                merchantName, rawLabel, minAmount, maxAmount, transactionType, category,
                description, Cadence.OCCASIONAL, 1, minHour, maxHour, 1
        );
    }

    private double randomAmount(double minAmount, double maxAmount) {
        return ThreadLocalRandom.current().nextDouble(minAmount, maxAmount);
    }

    private int randomBetween(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private double roundAmount(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    private <T> T randomItem(List<T> items) {
        return items.get(ThreadLocalRandom.current().nextInt(items.size()));
    }

    private String normalizeHolderName(String holderName) {
        String normalized = holderName == null ? "" : holderName.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? "SANDBOX USER" : normalized.toUpperCase(Locale.ROOT);
    }

    private enum Cadence {
        MONTHLY,
        BIWEEKLY,
        WEEKLY,
        OCCASIONAL
    }

    private record ProfileScenario(
            List<TransactionTemplate> recurringTemplates,
            List<TransactionTemplate> variableTemplates
    ) {
    }

    private record TransactionTemplate(
            String merchantName,
            String rawLabel,
            double minAmount,
            double maxAmount,
            SandboxTransactionType transactionType,
            TransactionCategory category,
            String description,
            Cadence cadence,
            int occurrences,
            int minHour,
            int maxHour,
            int baseOffsetDays
    ) {
    }
}



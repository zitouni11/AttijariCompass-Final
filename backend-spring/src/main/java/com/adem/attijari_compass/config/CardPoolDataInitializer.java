package com.adem.attijari_compass.config;

import com.adem.attijari_compass.entity.CardCatalog;
import com.adem.attijari_compass.entity.CardPool;
import com.adem.attijari_compass.entity.CardPoolTransaction;
import com.adem.attijari_compass.entity.TransactionStatus;
import com.adem.attijari_compass.repository.CardCatalogRepository;
import com.adem.attijari_compass.repository.CardPoolRepository;
import com.adem.attijari_compass.repository.CardPoolTransactionRepository;
import com.adem.attijari_compass.util.CardMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE - 90)
@Slf4j
public class CardPoolDataInitializer implements CommandLineRunner {

    private static final List<String> REQUESTED_CATALOG_CODES = List.of(
            "CARTE_FLEX",
            "CARTE_PLATINUM",
            "CARTE_GOLD_NATIONALE",
            "CARTE_GOLD_INTERNATIONALE",
            "CARTE_VISA_NATIONALE",
            "CARTE_VISA_INTERNATIONALE",
            "CARTE_CIB",
            "CARTE_VOYAGE",
            "CARTE_TECHNOLOGIQUE",
            "CARTE_AVENIR"
    );

    private static final List<String> FIRST_NAMES = List.of(
            "MOHAMED", "AHMED", "YASSINE", "AMINE", "KARIM",
            "SKANDER", "SAMI", "WALID", "MEHDI", "HATEM",
            "AYA", "LEILA", "MARIEM", "NOUR", "RANIA",
            "SALMA", "CHAIMA", "INES", "OLFA", "SARRA"
    );

    private static final List<String> LAST_NAMES = List.of(
            "BEN SALAH", "TRABELSI", "BOUAZIZI", "GHARBI", "MEJRI",
            "JAZIRI", "AMMAR", "CHAABANE", "MAALEJ", "SLIM",
            "KHELIFI", "AYARI", "HAMDI", "DRIDI", "FEKI",
            "BEN AMOR", "SASSI", "LAHMAR", "BEN ALI", "MANSOURI"
    );

    private final CardCatalogRepository cardCatalogRepository;
    private final CardPoolRepository cardPoolRepository;
    private final CardPoolTransactionRepository cardPoolTransactionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        LocalDateTime referenceDate = LocalDateTime.now().withSecond(0).withNano(0);
        Map<String, CardCatalog> catalogsByCode = loadRequestedCatalogs();
        Map<String, List<CardPoolTransactionTemplate>> templatesByCatalogCode = templatesByCatalogCode();
        List<CardPoolSeed> seeds = buildCardPoolSeeds();

        int seededCards = 0;
        int insertedTransactions = 0;
        int updatedTransactions = 0;

        for (CardPoolSeed seed : seeds) {
            CardCatalog cardCatalog = catalogsByCode.get(seed.cardCatalogCode());
            CardPool cardPool = upsertCardPool(cardCatalog, seed);
            seededCards++;

            SeedSyncResult result = syncTransactions(
                    cardPool,
                    templatesByCatalogCode.get(seed.cardCatalogCode()),
                    referenceDate
            );
            insertedTransactions += result.inserted();
            updatedTransactions += result.updated();
        }

        log.info(
                "Card pool ready: {} cards checked across {} catalog entries, {} transactions inserted, {} transactions synchronized",
                seededCards,
                catalogsByCode.size(),
                insertedTransactions,
                updatedTransactions
        );
    }

    private Map<String, CardCatalog> loadRequestedCatalogs() {
        Map<String, CardCatalog> activeCatalogsByCode = new LinkedHashMap<>();
        for (CardCatalog cardCatalog : cardCatalogRepository.findAllByActiveTrueOrderByNameAsc()) {
            activeCatalogsByCode.put(cardCatalog.getCode(), cardCatalog);
        }

        List<String> missingCatalogs = new ArrayList<>();
        Map<String, CardCatalog> requestedCatalogs = new LinkedHashMap<>();

        for (String catalogCode : REQUESTED_CATALOG_CODES) {
            CardCatalog cardCatalog = activeCatalogsByCode.get(catalogCode);
            if (cardCatalog == null) {
                missingCatalogs.add(catalogCode);
                continue;
            }
            requestedCatalogs.put(catalogCode, cardCatalog);
        }

        if (!missingCatalogs.isEmpty()) {
            throw new IllegalStateException("Missing active card catalogs for codes: " + String.join(", ", missingCatalogs));
        }

        log.info("Card pool seed validated against card_catalog for {} codes", requestedCatalogs.size());
        return requestedCatalogs;
    }

    private List<CardPoolSeed> buildCardPoolSeeds() {
        List<CardPoolSeed> seeds = new ArrayList<>();
        int generatedIdentityIndex = 0;

        for (int catalogIndex = 0; catalogIndex < REQUESTED_CATALOG_CODES.size(); catalogIndex++) {
            String catalogCode = REQUESTED_CATALOG_CODES.get(catalogIndex);
            seeds.add(knownSeedFor(catalogCode));

            int targetCount = resolveTargetCount(catalogCode);
            for (int slot = 2; slot <= targetCount; slot++) {
                HolderIdentity identity = resolveHolderIdentity(generatedIdentityIndex++);
                seeds.add(card(
                        catalogCode,
                        identity.fullName(),
                        generateCardNumber(catalogCode, catalogIndex, slot),
                        resolveExpiryMonth(catalogIndex, slot),
                        resolveExpiryYear(catalogIndex, slot),
                        resolveCvv(catalogCode, catalogIndex, slot),
                        generateCardCode(catalogCode, identity.cardCodeToken(), slot)
                ));
            }
        }

        return seeds;
    }

    private CardPoolSeed knownSeedFor(String catalogCode) {
        return switch (catalogCode) {
            case "CARTE_FLEX" -> card("CARTE_FLEX", "ADAM ZITOUNI", "4587123412344589", 12, 2028, "421", "FLEX-ADAM-01");
            case "CARTE_PLATINUM" -> card("CARTE_PLATINUM", "AYA BEN SALAH", "5378123498761234", 10, 2029, "315", "PLAT-AYA-01");
            case "CARTE_GOLD_NATIONALE" ->
                    card("CARTE_GOLD_NATIONALE", "SAMI TRABELSI", "5299456712348899", 8, 2027, "907", "GOLD-SAMI-01");
            case "CARTE_GOLD_INTERNATIONALE" ->
                    card("CARTE_GOLD_INTERNATIONALE", "YASSINE MEJRI", "5299456712347711", 3, 2030, "614", "GINT-YASSINE-01");
            case "CARTE_VISA_NATIONALE" ->
                    card("CARTE_VISA_NATIONALE", "MARIEM BOUAZIZ", "4111222233334444", 11, 2028, "552", "VNAT-MARIEM-01");
            case "CARTE_VISA_INTERNATIONALE" ->
                    card("CARTE_VISA_INTERNATIONALE", "NOUR GHARBI", "4556332211007788", 9, 2029, "228", "VINT-NOUR-01");
            case "CARTE_CIB" -> card("CARTE_CIB", "HATEM MAALEJ", "6270101234567890", 5, 2028, null, "CIB-HATEM-01");
            case "CARTE_VOYAGE" -> card("CARTE_VOYAGE", "LEILA CHAABANE", "4123456789123456", 6, 2029, null, "VOY-LEILA-01");
            case "CARTE_TECHNOLOGIQUE" ->
                    card("CARTE_TECHNOLOGIQUE", "KARIM JAZIRI", "4567987612341122", 4, 2028, null, "TECH-KARIM-01");
            case "CARTE_AVENIR" -> card("CARTE_AVENIR", "SKANDER AMMAR", "4532111100009876", 1, 2029, null, "AVEN-SKANDER-01");
            default -> throw new IllegalArgumentException("Unsupported catalog code for card pool seed: " + catalogCode);
        };
    }

    private int resolveTargetCount(String catalogCode) {
        return switch (catalogCode) {
            case "CARTE_FLEX" -> 14;
            case "CARTE_PLATINUM" -> 8;
            case "CARTE_GOLD_NATIONALE" -> 12;
            case "CARTE_GOLD_INTERNATIONALE" -> 8;
            case "CARTE_VISA_NATIONALE" -> 12;
            case "CARTE_VISA_INTERNATIONALE" -> 10;
            case "CARTE_CIB" -> 10;
            case "CARTE_VOYAGE" -> 8;
            case "CARTE_TECHNOLOGIQUE" -> 8;
            case "CARTE_AVENIR" -> 10;
            default -> throw new IllegalArgumentException("Unsupported catalog code for target count: " + catalogCode);
        };
    }

    private HolderIdentity resolveHolderIdentity(int generatedIdentityIndex) {
        String firstName = FIRST_NAMES.get(generatedIdentityIndex % FIRST_NAMES.size());
        String lastName = LAST_NAMES.get((generatedIdentityIndex / FIRST_NAMES.size()) % LAST_NAMES.size());
        return new HolderIdentity(firstName + " " + lastName, firstName);
    }

    private String generateCardNumber(String catalogCode, int catalogIndex, int slot) {
        long suffix = 1_000_000_000L + ((long) catalogIndex + 1L) * 10_000L + (long) slot * 137L;
        return resolveCardNumberPrefix(catalogCode) + String.format("%010d", suffix);
    }

    private String generateCardCode(String catalogCode, String holderToken, int slot) {
        return resolveCardCodePrefix(catalogCode) + "-" + toCodeToken(holderToken) + "-" + String.format("%02d", slot);
    }

    private int resolveExpiryMonth(int catalogIndex, int slot) {
        return ((catalogIndex * 3) + slot - 1) % 12 + 1;
    }

    private int resolveExpiryYear(int catalogIndex, int slot) {
        return 2027 + ((catalogIndex + slot) % 5);
    }

    private String resolveCvv(String catalogCode, int catalogIndex, int slot) {
        if ("CARTE_CIB".equals(catalogCode) && slot % 2 == 0) {
            return null;
        }
        if (("CARTE_VOYAGE".equals(catalogCode) || "CARTE_TECHNOLOGIQUE".equals(catalogCode)) && slot % 3 == 0) {
            return null;
        }
        if ("CARTE_AVENIR".equals(catalogCode) && slot % 4 == 0) {
            return null;
        }

        int value = 100 + (((catalogIndex + 1) * 43 + slot * 17) % 900);
        return String.format("%03d", value);
    }

    private String resolveCardNumberPrefix(String catalogCode) {
        return switch (catalogCode) {
            case "CARTE_FLEX" -> "458712";
            case "CARTE_PLATINUM" -> "537812";
            case "CARTE_GOLD_NATIONALE" -> "529945";
            case "CARTE_GOLD_INTERNATIONALE" -> "529946";
            case "CARTE_VISA_NATIONALE" -> "411122";
            case "CARTE_VISA_INTERNATIONALE" -> "455633";
            case "CARTE_CIB" -> "627010";
            case "CARTE_VOYAGE" -> "412345";
            case "CARTE_TECHNOLOGIQUE" -> "456798";
            case "CARTE_AVENIR" -> "453211";
            default -> throw new IllegalArgumentException("Unsupported catalog code for card number prefix: " + catalogCode);
        };
    }

    private String resolveCardCodePrefix(String catalogCode) {
        return switch (catalogCode) {
            case "CARTE_FLEX" -> "FLEX";
            case "CARTE_PLATINUM" -> "PLAT";
            case "CARTE_GOLD_NATIONALE" -> "GOLDN";
            case "CARTE_GOLD_INTERNATIONALE" -> "GOLDI";
            case "CARTE_VISA_NATIONALE" -> "VNAT";
            case "CARTE_VISA_INTERNATIONALE" -> "VINT";
            case "CARTE_CIB" -> "CIB";
            case "CARTE_VOYAGE" -> "VOY";
            case "CARTE_TECHNOLOGIQUE" -> "TECH";
            case "CARTE_AVENIR" -> "AVEN";
            default -> throw new IllegalArgumentException("Unsupported catalog code for card code prefix: " + catalogCode);
        };
    }

    private String toCodeToken(String value) {
        String token = value == null
                ? "CARD"
                : value.trim().replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        return token.isBlank() ? "CARD" : token;
    }

    private CardPool upsertCardPool(CardCatalog cardCatalog, CardPoolSeed seed) {
        CardPool cardPool = cardPoolRepository.findByCardCode(seed.cardCode()).orElseGet(CardPool::new);
        boolean alreadyAssigned = cardPool.getId() != null && cardPool.isAssigned();
        Long assignedUserId = cardPool.getAssignedUserId();

        cardPool.setCardCatalog(cardCatalog);
        cardPool.setCardHolderName(normalizeHolderName(seed.cardHolderName()));
        cardPool.setCardNumber(CardMaskingUtil.normalizeCardNumber(seed.cardNumber()));
        cardPool.setExpiryMonth(seed.expiryMonth());
        cardPool.setExpiryYear(seed.expiryYear());
        cardPool.setCvv(seed.cvv());
        cardPool.setMaskedCardNumber(CardMaskingUtil.maskCardNumber(seed.cardNumber()));
        cardPool.setLast4(CardMaskingUtil.extractLast4(seed.cardNumber()));
        cardPool.setCardCode(seed.cardCode());
        cardPool.setAssigned(alreadyAssigned);
        cardPool.setAssignedUserId(alreadyAssigned ? assignedUserId : null);

        return cardPoolRepository.save(cardPool);
    }

    private SeedSyncResult syncTransactions(
            CardPool cardPool,
            List<CardPoolTransactionTemplate> templates,
            LocalDateTime referenceDate) {
        if (templates == null || templates.isEmpty()) {
            return new SeedSyncResult(0, 0);
        }

        List<CardPoolTransaction> desiredTransactions = buildTransactions(cardPool, templates, referenceDate);
        Map<String, CardPoolTransaction> existingByReference = new LinkedHashMap<>();

        for (CardPoolTransaction existing : cardPoolTransactionRepository.findAllByCardPoolIdOrderByTransactionDateDesc(cardPool.getId())) {
            if (existing.getExternalReference() != null) {
                existingByReference.put(existing.getExternalReference(), existing);
            }
        }

        List<CardPoolTransaction> transactionsToSave = new ArrayList<>();
        int inserted = 0;
        int updated = 0;

        for (CardPoolTransaction desired : desiredTransactions) {
            CardPoolTransaction target = desired.getExternalReference() == null
                    ? null
                    : existingByReference.get(desired.getExternalReference());

            if (target == null) {
                target = new CardPoolTransaction();
                inserted++;
            } else {
                updated++;
            }

            mergeTransaction(target, desired, cardPool);
            transactionsToSave.add(target);
        }

        if (!transactionsToSave.isEmpty()) {
            cardPoolTransactionRepository.saveAll(transactionsToSave);
        }

        return new SeedSyncResult(inserted, updated);
    }

    private void mergeTransaction(CardPoolTransaction target, CardPoolTransaction source, CardPool cardPool) {
        target.setCardPool(cardPool);
        target.setTransactionDate(source.getTransactionDate());
        target.setValueDate(source.getValueDate());
        target.setAmount(source.getAmount());
        target.setMerchantName(source.getMerchantName());
        target.setDescription(source.getDescription());
        target.setReference(source.getReference());
        target.setCategory(source.getCategory());
        target.setStatus(source.getStatus());
        target.setCity(source.getCity());
        target.setCountry(source.getCountry());
        target.setCurrency("TND");
        target.setInstallment(source.isInstallment());
        target.setInstallmentIndex(source.getInstallmentIndex());
        target.setInstallmentTotal(source.getInstallmentTotal());
        target.setExternalReference(source.getExternalReference());
    }

    private List<CardPoolTransaction> buildTransactions(
            CardPool cardPool,
            List<CardPoolTransactionTemplate> templates,
            LocalDateTime referenceDate) {
        List<CardPoolTransaction> transactions = new ArrayList<>();
        int sequence = 1;

        for (int cycle = 2; cycle >= 0; cycle--) {
            for (CardPoolTransactionTemplate template : templates) {
                LocalDateTime transactionDate = referenceDate.minusDays((long) cycle * 28 + template.daysAgo())
                        .withHour(template.hour())
                        .withMinute(template.minute())
                        .withSecond(0)
                        .withNano(0);

                TransactionStatus status = template.pendingOnLatestCycle() && cycle == 0
                        ? TransactionStatus.PENDING
                        : TransactionStatus.POSTED;

                LocalDateTime valueDate = status == TransactionStatus.PENDING
                        ? null
                        : transactionDate.plusDays(template.valueDateShiftDays());

                BigDecimal amount = template.baseAmount()
                        .add(template.amountStep().multiply(BigDecimal.valueOf(2L - cycle)))
                        .setScale(2, RoundingMode.HALF_UP);

                Integer installmentIndex = template.installment() ? 3 - cycle : null;
                Integer installmentTotal = template.installment() ? template.installmentTotal() : null;

                transactions.add(CardPoolTransaction.builder()
                        .cardPool(cardPool)
                        .transactionDate(transactionDate)
                        .valueDate(valueDate)
                        .amount(amount)
                        .merchantName(template.merchantName())
                        .description(template.description())
                        .reference(cardPool.getCardCode() + "-" + template.referencePrefix() + "-" + String.format("%03d", sequence))
                        .category(template.category())
                        .status(status)
                        .city(template.city())
                        .country(template.country())
                        .currency("TND")
                        .installment(template.installment())
                        .installmentIndex(installmentIndex)
                        .installmentTotal(installmentTotal)
                        .externalReference(cardPool.getCardCode() + "-TX-" + String.format("%03d", sequence))
                        .build());
                sequence++;
            }
        }

        return transactions;
    }

    private Map<String, List<CardPoolTransactionTemplate>> templatesByCatalogCode() {
        Map<String, List<CardPoolTransactionTemplate>> templates = new LinkedHashMap<>();
        templates.put("CARTE_FLEX", flexTemplates());
        templates.put("CARTE_PLATINUM", premiumTemplates());
        templates.put("CARTE_GOLD_NATIONALE", localEverydayTemplates());
        templates.put("CARTE_GOLD_INTERNATIONALE", internationalTemplates());
        templates.put("CARTE_VISA_NATIONALE", localEverydayTemplates());
        templates.put("CARTE_VISA_INTERNATIONALE", internationalTemplates());
        templates.put("CARTE_CIB", essentialTemplates());
        templates.put("CARTE_TAWA_TAWA", youthTemplates());
        templates.put("CARTE_IDDIKHAR", savingsTemplates());
        templates.put("CARTE_VOYAGE", voyageTemplates());
        templates.put("CARTE_OULIDHA", youthTemplates());
        templates.put("CARTE_TECHNOLOGIQUE", digitalTemplates());
        templates.put("CARTE_AVENIR", youthTemplates());
        return templates;
    }

    private List<CardPoolTransactionTemplate> flexTemplates() {
        return List.of(
                tx("Alimentation carte", "Versement sur carte Flex", "Versement", "Tunis", "Tunisie", "500.00", "20.00", 1, 8, 5, false, 0, false, 0, "ALIM"),
                tx("Fnac Tunis", "Paiement ordinateur en 3 fois", "Shopping", "Tunis", "Tunisie", "-800.00", "-15.00", 2, 18, 15, true, 3, false, 1, "FNAC"),
                tx("Zara Tunis City", "Achat mode saisonnier", "Mode", "Tunis", "Tunisie", "-185.00", "-4.00", 3, 17, 25, false, 0, false, 0, "ZARA"),
                tx("Virement recu", "Virement recu sur carte Flex", "Virement", "Tunis", "Tunisie", "800.00", "25.00", 4, 9, 10, false, 0, false, 0, "VIRM"),
                tx("Tunisianet", "Accessoires high-tech", "E-commerce", "Tunis", "Tunisie", "-128.00", "-3.00", 6, 20, 40, false, 0, false, 1, "TNET"),
                tx("Carrefour La Marsa", "Courses mensuelles", "Grande distribution", "La Marsa", "Tunisie", "-120.00", "-3.00", 7, 19, 10, false, 0, false, 0, "CRFR"),
                tx("Cashback Attijari", "Cashback mensuel carte Flex", "Cashback", "Tunis", "Tunisie", "15.00", "0.00", 8, 9, 20, false, 0, false, 0, "CASH"),
                tx("Uber", "Trajet domicile bureau", "Transport", "Tunis", "Tunisie", "-18.00", "-0.40", 10, 8, 20, false, 0, true, 0, "UBER"),
                tx("Bolt", "Trajet retour soiree", "Transport", "Tunis", "Tunisie", "-12.00", "-0.30", 11, 22, 5, false, 0, false, 0, "BOLT"),
                tx("Remboursement commercant", "Remboursement partiel achat Fnac", "Remboursement", "Tunis", "Tunisie", "120.00", "0.00", 12, 10, 45, false, 0, false, 0, "RMBT"),
                tx("MG City", "Courses express", "Grande distribution", "Ariana", "Tunisie", "-43.00", "-1.10", 14, 18, 55, false, 0, false, 0, "MGCY"),
                tx("Shell Lac 1", "Carburant", "Carburant", "Tunis", "Tunisie", "-92.00", "-2.30", 18, 7, 45, false, 0, false, 0, "SHLL"),
                tx("Monoprix Ennasr", "Produits maison", "Grande distribution", "Ariana", "Tunisie", "-58.00", "-1.50", 24, 19, 30, false, 0, false, 0, "MNPR")
        );
    }

    private List<CardPoolTransactionTemplate> premiumTemplates() {
        return List.of(
                tx("Salaire", "Salaire mensuel premium", "Revenus", "Tunis", "Tunisie", "3500.00", "75.00", 1, 8, 0, false, 0, false, 0, "SALA"),
                tx("Virement recu", "Virement recu partenaire", "Virement", "Tunis", "Tunisie", "800.00", "20.00", 3, 10, 10, false, 0, false, 0, "VIRM"),
                tx("Cashback Premium", "Cashback avantages premium", "Cashback", "Tunis", "Tunisie", "35.00", "0.50", 5, 9, 30, false, 0, false, 0, "CASH"),
                tx("Movenpick Gammarth", "Sejour weekend premium", "Hotellerie", "Gammarth", "Tunisie", "-920.00", "-22.00", 7, 12, 15, false, 0, false, 1, "MOVP"),
                tx("Dar El Jeld", "Diner gastronomique", "Restaurant premium", "Tunis", "Tunisie", "-245.00", "-6.50", 9, 21, 10, false, 0, false, 0, "JELD"),
                tx("Four Seasons Tunis", "Reservation spa et soins", "Bien-etre", "Gammarth", "Tunisie", "-310.00", "-9.00", 11, 15, 40, false, 0, false, 0, "FOUR"),
                tx("Royal Air Maroc", "Billet premium regional", "Voyage", "Tunis", "Tunisie", "-640.00", "-15.00", 14, 10, 35, false, 0, false, 1, "RAM"),
                tx("Sheraton Tunis", "Meeting et restauration hotel", "Hotellerie", "Tunis", "Tunisie", "-430.00", "-11.00", 17, 14, 20, false, 0, false, 0, "SHRT"),
                tx("Zitouna Mall Luxe", "Shopping premium", "Shopping", "Tunis", "Tunisie", "-520.00", "-14.00", 20, 18, 25, false, 0, false, 0, "SHOP"),
                tx("Le Golfe", "Diner vue mer", "Restaurant premium", "La Marsa", "Tunisie", "-268.00", "-7.00", 24, 21, 40, false, 0, true, 0, "GOLF")
        );
    }

    private List<CardPoolTransactionTemplate> localEverydayTemplates() {
        return List.of(
                tx("Salaire", "Salaire mensuel verse sur compte", "Revenus", "Tunis", "Tunisie", "3500.00", "50.00", 1, 8, 5, false, 0, false, 0, "SALA"),
                tx("Virement recu", "Virement recu d'un proche", "Virement", "Tunis", "Tunisie", "800.00", "15.00", 3, 10, 15, false, 0, false, 0, "VIRM"),
                tx("Cashback Attijari", "Cashback mensuel de fidelite", "Cashback", "Tunis", "Tunisie", "20.00", "0.00", 5, 9, 0, false, 0, false, 0, "CASH"),
                tx("Carrefour Market", "Courses hebdomadaires", "Grande distribution", "Tunis", "Tunisie", "-120.00", "-2.80", 7, 18, 30, false, 0, false, 0, "CRFM"),
                tx("Monoprix Lafayette", "Courses et produits maison", "Grande distribution", "Tunis", "Tunisie", "-86.00", "-2.00", 9, 19, 5, false, 0, false, 0, "MNPL"),
                tx("MG", "Produits frais", "Grande distribution", "Ariana", "Tunisie", "-58.00", "-1.20", 11, 17, 45, false, 0, false, 0, "MG"),
                tx("Orange", "Forfait internet maison", "Telecom", "Tunis", "Tunisie", "-39.00", "0.00", 14, 8, 15, false, 0, false, 0, "ORNG"),
                tx("Uber", "Trajet quotidien", "Transport", "Tunis", "Tunisie", "-18.00", "-0.40", 17, 8, 20, false, 0, true, 0, "UBER"),
                tx("Total Energies", "Plein carburant", "Carburant", "Tunis", "Tunisie", "-95.00", "-2.10", 20, 7, 25, false, 0, false, 0, "TOTAL"),
                tx("Pharmacie Centrale", "Produits pharmacie", "Sante", "Tunis", "Tunisie", "-47.00", "-1.30", 24, 13, 40, false, 0, false, 0, "PHRM")
        );
    }

    private List<CardPoolTransactionTemplate> internationalTemplates() {
        return List.of(
                tx("Virement recu", "Virement recu pour deplacement", "Virement", "Tunis", "Tunisie", "800.00", "20.00", 1, 9, 5, false, 0, false, 0, "VIRM"),
                tx("Cashback Voyage", "Cashback paiements internationaux", "Cashback", "Tunis", "Tunisie", "20.00", "0.00", 3, 9, 45, false, 0, false, 0, "CASH"),
                tx("Tunisair", "Billet aller-retour", "Voyage", "Tunis", "Tunisie", "-560.00", "-12.00", 5, 10, 10, false, 0, false, 1, "TAIR"),
                tx("Booking", "Reservation hotel a l'etranger", "Hotellerie", "Tunis", "Tunisie", "-420.00", "-9.00", 7, 14, 15, false, 0, false, 1, "BOOK"),
                tx("Uber", "Trajet aeroport hotel", "Transport", "Paris", "France", "-18.00", "-0.60", 10, 22, 10, false, 0, true, 0, "UBER"),
                tx("Duty Free Tunis", "Achats voyage", "Shopping", "Tunis", "Tunisie", "-144.00", "-4.20", 12, 6, 20, false, 0, false, 0, "DUTY"),
                tx("Air France", "Complement billet voyage", "Voyage", "Paris", "France", "-310.00", "-7.50", 15, 13, 35, false, 0, false, 1, "AFRN"),
                tx("Hotel Paris", "Pre-autorisation sejour", "Hotellerie", "Paris", "France", "-780.00", "-18.00", 18, 20, 30, false, 0, false, 1, "HPRS"),
                tx("Fnac", "Achat accessoires voyage", "Shopping", "Paris", "France", "-190.00", "-4.00", 21, 17, 25, false, 0, false, 0, "FNAC"),
                tx("Booking Refund", "Remboursement hotel", "Remboursement", "Paris", "France", "120.00", "0.00", 24, 9, 45, false, 0, false, 0, "RFND")
        );
    }

    private List<CardPoolTransactionTemplate> essentialTemplates() {
        return List.of(
                tx("Pension", "Versement pension mensuelle", "Revenus", "Tunis", "Tunisie", "1800.00", "30.00", 1, 8, 5, false, 0, false, 0, "PENS"),
                tx("Virement recu", "Virement recu compte principal", "Virement", "Tunis", "Tunisie", "400.00", "10.00", 3, 10, 10, false, 0, false, 0, "VIRM"),
                tx("Cashback Attijari", "Cashback operations locales", "Cashback", "Tunis", "Tunisie", "12.00", "0.00", 5, 9, 35, false, 0, false, 0, "CASH"),
                tx("Attijari GAB", "Retrait espece", "Retrait", "Tunis", "Tunisie", "-60.00", "-5.00", 7, 9, 10, false, 0, false, 0, "GAB"),
                tx("Carrefour Market", "Courses de base", "Grande distribution", "Tunis", "Tunisie", "-48.00", "-1.00", 9, 18, 20, false, 0, false, 0, "CRFM"),
                tx("MG", "Produits essentiels", "Grande distribution", "Ariana", "Tunisie", "-33.00", "-0.70", 11, 17, 30, false, 0, false, 0, "MG"),
                tx("Tunisie Telecom", "Facture telephone", "Telecom", "Tunis", "Tunisie", "-29.00", "0.00", 14, 10, 10, false, 0, false, 0, "TT"),
                tx("Pharmacie Centrale", "Produits sante", "Sante", "Tunis", "Tunisie", "-27.00", "-0.60", 17, 12, 15, false, 0, false, 0, "PHRM"),
                tx("Total Energies", "Carburant deplacement utile", "Carburant", "Tunis", "Tunisie", "-54.00", "-1.10", 20, 7, 45, false, 0, false, 0, "TOTL"),
                tx("Patisserie Masmoudi", "Achat occasion familiale", "Gastronomie", "Sfax", "Tunisie", "-18.00", "-0.40", 24, 16, 50, false, 0, true, 0, "MASM")
        );
    }

    private List<CardPoolTransactionTemplate> savingsTemplates() {
        return List.of(
                tx("Pension", "Versement regulier epargne", "Revenus", "Tunis", "Tunisie", "1800.00", "25.00", 1, 8, 5, false, 0, false, 0, "PENS"),
                tx("Virement recu", "Virement recu famille", "Virement", "Tunis", "Tunisie", "400.00", "10.00", 3, 10, 10, false, 0, false, 0, "VIRM"),
                tx("Cashback Attijari", "Cashback utilisation responsable", "Cashback", "Tunis", "Tunisie", "12.00", "0.00", 5, 9, 35, false, 0, false, 0, "CASH"),
                tx("Carrefour Market", "Courses de base", "Grande distribution", "Tunis", "Tunisie", "-48.00", "-1.00", 7, 18, 20, false, 0, false, 0, "CRFM"),
                tx("MG", "Produits essentiels", "Grande distribution", "Ariana", "Tunisie", "-33.00", "-0.70", 9, 17, 30, false, 0, false, 0, "MG"),
                tx("Pharmacie Centrale", "Produits sante", "Sante", "Tunis", "Tunisie", "-27.00", "-0.60", 11, 12, 15, false, 0, false, 0, "PHRM"),
                tx("Tunisie Telecom", "Facture telephone", "Telecom", "Tunis", "Tunisie", "-29.00", "0.00", 14, 10, 10, false, 0, false, 0, "TT"),
                tx("Attijari GAB", "Retrait ponctuel", "Retrait", "Tunis", "Tunisie", "-60.00", "-5.00", 17, 8, 10, false, 0, false, 0, "GAB"),
                tx("Total Energies", "Carburant utilitaire", "Carburant", "Tunis", "Tunisie", "-54.00", "-1.10", 20, 7, 45, false, 0, false, 0, "TOTL"),
                tx("Orange", "Recharge internet mobile", "Telecom", "Tunis", "Tunisie", "-10.00", "0.00", 24, 13, 25, false, 0, true, 0, "ORNG")
        );
    }

    private List<CardPoolTransactionTemplate> youthTemplates() {
        return List.of(
                tx("Virement recu", "Virement recu parents", "Virement", "Tunis", "Tunisie", "200.00", "10.00", 1, 8, 30, false, 0, false, 0, "VIRM"),
                tx("Cashback Attijari", "Cashback paiements jeunesse", "Cashback", "Tunis", "Tunisie", "10.00", "0.00", 3, 9, 20, false, 0, false, 0, "CASH"),
                tx("LC Waikiki", "Vetements casual", "Mode", "Tunis", "Tunisie", "-64.00", "-1.50", 5, 17, 20, false, 0, false, 0, "LCWK"),
                tx("Monoprix", "Snacks et produits perso", "Grande distribution", "Tunis", "Tunisie", "-24.00", "-0.50", 7, 20, 25, false, 0, false, 0, "MNPR"),
                tx("Orange", "Recharge data", "Telecom", "Tunis", "Tunisie", "-12.00", "0.00", 10, 11, 5, false, 0, false, 0, "ORNG"),
                tx("Bolt", "Trajet urbain", "Transport", "Tunis", "Tunisie", "-9.00", "-0.30", 12, 19, 40, false, 0, false, 0, "BOLT"),
                tx("Fnac Tunis", "Accessoires et fournitures", "Shopping", "Tunis", "Tunisie", "-28.00", "-0.60", 15, 16, 15, false, 0, false, 0, "FNAC"),
                tx("Patisserie Masmoudi", "Pause douceur", "Gastronomie", "Sfax", "Tunisie", "-15.00", "-0.40", 18, 17, 10, false, 0, false, 0, "MASM"),
                tx("Carrefour Market", "Achats du quotidien", "Grande distribution", "Ariana", "Tunisie", "-22.00", "-0.70", 21, 18, 35, false, 0, false, 0, "CRFM"),
                tx("CineMadart", "Sortie cinema", "Loisirs", "Tunis", "Tunisie", "-18.00", "-0.50", 24, 21, 10, false, 0, true, 0, "CINE")
        );
    }

    private List<CardPoolTransactionTemplate> voyageTemplates() {
        return List.of(
                tx("Allocation voyage", "Recharge allocation touristique", "Virement", "Tunis", "Tunisie", "1200.00", "25.00", 1, 8, 10, false, 0, false, 0, "ALLO"),
                tx("Booking Refund", "Remboursement ajustement reservation", "Remboursement", "Tunis", "Tunisie", "85.00", "0.00", 3, 9, 30, false, 0, false, 0, "RFND"),
                tx("Tunisair", "Billet aller-retour loisirs", "Voyage", "Tunis", "Tunisie", "-560.00", "-12.00", 5, 10, 10, false, 0, false, 1, "TAIR"),
                tx("Booking Tunisie", "Reservation hotel a l'etranger", "Hotellerie", "Tunis", "Tunisie", "-420.00", "-9.00", 7, 14, 15, false, 0, false, 1, "BOOK"),
                tx("Uber Aeroport", "Trajet aeroport centre-ville", "Transport", "Tunis", "Tunisie", "-24.00", "-0.80", 10, 22, 10, false, 0, true, 0, "UBRP"),
                tx("Shell Autoroute", "Carburant trajet long", "Carburant", "Sousse", "Tunisie", "-125.00", "-3.00", 12, 7, 40, false, 0, false, 0, "SHEL"),
                tx("Movenpick Sousse", "Sejour vacances", "Hotellerie", "Sousse", "Tunisie", "-690.00", "-16.00", 15, 19, 25, false, 0, false, 1, "MVSO"),
                tx("Duty Free Tunis", "Achats voyage", "Shopping", "Tunis", "Tunisie", "-144.00", "-4.20", 18, 6, 20, false, 0, false, 0, "DUTY"),
                tx("Air France", "Complement billet voyage", "Voyage", "Tunis", "Tunisie", "-310.00", "-7.50", 21, 13, 35, false, 0, false, 1, "AFRN"),
                tx("Hotel Paris", "Pre-autorisation sejour", "Hotellerie", "Paris", "France", "-780.00", "-18.00", 24, 20, 30, false, 0, false, 1, "HPRS")
        );
    }

    private List<CardPoolTransactionTemplate> digitalTemplates() {
        return List.of(
                tx("Virement freelance", "Paiement mission digitale", "Revenus", "Tunis", "Tunisie", "2500.00", "50.00", 1, 8, 0, false, 0, false, 0, "FREE"),
                tx("Virement recu", "Remboursement client", "Virement", "Tunis", "Tunisie", "800.00", "20.00", 3, 10, 10, false, 0, false, 0, "VIRM"),
                tx("Cashback Digital", "Cashback services en ligne", "Cashback", "Tunis", "Tunisie", "20.00", "0.00", 5, 9, 25, false, 0, false, 0, "CASH"),
                tx("OpenAI", "Credits API intelligence artificielle", "Services digitaux", "Tunis", "Tunisie", "-44.00", "-2.00", 7, 6, 20, false, 0, false, 0, "OPEN"),
                tx("GitHub", "Abonnement organisation privee", "Developpement", "Tunis", "Tunisie", "-24.00", "0.00", 9, 7, 15, false, 0, false, 0, "GIT"),
                tx("Figma", "Licence design collaborative", "Productivite", "Tunis", "Tunisie", "-18.00", "0.00", 11, 8, 10, false, 0, false, 0, "FIGM"),
                tx("Adobe", "Abonnement Creative Cloud", "Design", "Tunis", "Tunisie", "-36.00", "-0.50", 14, 7, 50, false, 0, false, 0, "ADBE"),
                tx("Google Workspace", "Services email et stockage", "Cloud", "Tunis", "Tunisie", "-28.00", "-0.80", 17, 8, 40, false, 0, false, 0, "GOOG"),
                tx("Microsoft Azure", "Infrastructure cloud staging", "Cloud", "Tunis", "Tunisie", "-62.00", "-2.50", 20, 9, 10, false, 0, false, 0, "AZUR"),
                tx("AWS", "Services compute et monitoring", "Cloud", "Tunis", "Tunisie", "-116.00", "-4.00", 24, 7, 45, false, 0, true, 0, "AWS")
        );
    }

    private CardPoolSeed card(
            String cardCatalogCode,
            String cardHolderName,
            String cardNumber,
            int expiryMonth,
            int expiryYear,
            String cvv,
            String cardCode) {
        return new CardPoolSeed(cardCatalogCode, cardHolderName, cardNumber, expiryMonth, expiryYear, cvv, cardCode);
    }

    private CardPoolTransactionTemplate tx(
            String merchantName,
            String description,
            String category,
            String city,
            String country,
            String baseAmount,
            String amountStep,
            int daysAgo,
            int hour,
            int minute,
            boolean installment,
            int installmentTotal,
            boolean pendingOnLatestCycle,
            int valueDateShiftDays,
            String referencePrefix) {
        return new CardPoolTransactionTemplate(
                merchantName,
                description,
                category,
                city,
                country,
                new BigDecimal(baseAmount),
                new BigDecimal(amountStep),
                daysAgo,
                hour,
                minute,
                installment,
                installmentTotal,
                pendingOnLatestCycle,
                valueDateShiftDays,
                referencePrefix
        );
    }

    private String normalizeHolderName(String value) {
        return value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private record HolderIdentity(String fullName, String cardCodeToken) {
    }

    private record CardPoolSeed(
            String cardCatalogCode,
            String cardHolderName,
            String cardNumber,
            int expiryMonth,
            int expiryYear,
            String cvv,
            String cardCode) {
    }

    private record CardPoolTransactionTemplate(
            String merchantName,
            String description,
            String category,
            String city,
            String country,
            BigDecimal baseAmount,
            BigDecimal amountStep,
            int daysAgo,
            int hour,
            int minute,
            boolean installment,
            int installmentTotal,
            boolean pendingOnLatestCycle,
            int valueDateShiftDays,
            String referencePrefix) {
    }

    private record SeedSyncResult(int inserted, int updated) {
    }
}

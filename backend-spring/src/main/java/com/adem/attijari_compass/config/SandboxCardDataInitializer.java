package com.adem.attijari_compass.config;

import com.adem.attijari_compass.entity.CardStatus;
import com.adem.attijari_compass.entity.CardType;
import com.adem.attijari_compass.entity.SandboxTransactionType;
import com.adem.attijari_compass.entity.TestCardCatalog;
import com.adem.attijari_compass.entity.TestCardTransaction;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.repository.TestCardCatalogRepository;
import com.adem.attijari_compass.repository.TestCardTransactionRepository;
import com.adem.attijari_compass.util.CardMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SandboxCardDataInitializer implements CommandLineRunner {

    private final TestCardCatalogRepository testCardCatalogRepository;
    private final TestCardTransactionRepository testCardTransactionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        LocalDateTime referenceDate = LocalDateTime.now().withSecond(0).withNano(0);

        List<SandboxCardSeed> seeds = List.of(
                new SandboxCardSeed(
                        "Test User",
                        "4242424242424242",
                        12,
                        2028,
                        "123",
                        CardType.VISA,
                        "Attijari Bank Tunisie",
                        CardStatus.ACTIVE,
                        1_250.0,
                        "STUDENT",
                        studentTemplates()
                ),
                new SandboxCardSeed(
                        "Yasmine Ben Salem",
                        "5555555555554444",
                        11,
                        2029,
                        "456",
                        CardType.MASTERCARD,
                        "BIAT",
                        CardStatus.ACTIVE,
                        8_650.0,
                        "FAMILY",
                        familyTemplates()
                ),
                new SandboxCardSeed(
                        "Sami Trabelsi",
                        "4111111111111111",
                        8,
                        2030,
                        "321",
                        CardType.VISA,
                        "UIB",
                        CardStatus.ACTIVE,
                        14_800.0,
                        "SALARIED",
                        salariedTemplates()
                ),
                new SandboxCardSeed(
                        "Karim Jaziri",
                        "4000002500003155",
                        4,
                        2029,
                        "654",
                        CardType.VISA,
                        "Amen Bank",
                        CardStatus.ACTIVE,
                        6_200.0,
                        "FREELANCE",
                        freelancerTemplates()
                ),
                new SandboxCardSeed(
                        "Leila Chaabane",
                        "5200828282828210",
                        9,
                        2031,
                        "987",
                        CardType.MASTERCARD,
                        "Attijari Bank Tunisie",
                        CardStatus.ACTIVE,
                        40_200.0,
                        "PREMIUM",
                        premiumTemplates()
                ),
                new SandboxCardSeed(
                        "Expired Demo",
                        "4000000000000069",
                        1,
                        2025,
                        "111",
                        CardType.VISA,
                        "Zitouna Bank",
                        CardStatus.EXPIRED,
                        2_100.0,
                        "EXPIRED",
                        List.of()
                ),
                new SandboxCardSeed(
                        "Blocked Demo",
                        "4000000000000119",
                        6,
                        2029,
                        "222",
                        CardType.MASTERCARD,
                        "BH Bank",
                        CardStatus.BLOCKED,
                        3_100.0,
                        "BLOCKED",
                        List.of()
                )
        );

        int seededCards = 0;
        int seededTransactions = 0;

        for (SandboxCardSeed seed : seeds) {
            TestCardCatalog card = upsertCard(seed);
            seededCards++;
            seededTransactions += seedTransactionsIfMissing(card, seed.referencePrefix(), seed.templates(), referenceDate);
        }

        log.info("Sandbox card catalog ready: {} cards checked, {} transactions inserted", seededCards, seededTransactions);
    }

    private TestCardCatalog upsertCard(SandboxCardSeed seed) {
        TestCardCatalog card = testCardCatalogRepository.findByTestCardNumber(seed.cardNumber())
                .orElseGet(TestCardCatalog::new);

        card.setHolderName(seed.holderName());
        card.setTestCardNumber(seed.cardNumber());
        card.setMaskedCardNumber(CardMaskingUtil.maskCardNumber(seed.cardNumber()));
        card.setExpiryMonth(seed.expiryMonth());
        card.setExpiryYear(seed.expiryYear());
        card.setCvv(seed.cvv());
        card.setCardType(seed.cardType());
        card.setBankName(seed.bankName());
        card.setStatus(seed.status());
        card.setInitialBalance(seed.initialBalance());

        return testCardCatalogRepository.save(card);
    }

    private int seedTransactionsIfMissing(
            TestCardCatalog card,
            String referencePrefix,
            List<RecurringSandboxTransactionTemplate> templates,
            LocalDateTime referenceDate) {
        if (templates.isEmpty() || testCardTransactionRepository.countByTestCardId(card.getId()) > 0) {
            return 0;
        }

        List<TestCardTransaction> transactions = buildTransactions(card, referencePrefix, templates, referenceDate);
        testCardTransactionRepository.saveAll(transactions);
        return transactions.size();
    }

    private List<TestCardTransaction> buildTransactions(
            TestCardCatalog card,
            String referencePrefix,
            List<RecurringSandboxTransactionTemplate> templates,
            LocalDateTime referenceDate) {
        List<TestCardTransaction> transactions = new ArrayList<>();
        int sequence = 1;

        for (int cycle = 2; cycle >= 0; cycle--) {
            for (RecurringSandboxTransactionTemplate template : templates) {
                LocalDateTime transactionDate = referenceDate.minusDays((long) cycle * 28 + template.daysAgo())
                        .withHour(template.hour())
                        .withMinute(template.minute())
                        .withSecond(0)
                        .withNano(0);

                double amount = template.baseAmount() + (template.amountStep() * (2 - cycle));

                transactions.add(TestCardTransaction.builder()
                        .testCard(card)
                        .merchantName(template.merchantName())
                        .rawLabel(template.rawLabel())
                        .amount(amount)
                        .transactionType(template.transactionType())
                        .transactionDate(transactionDate)
                        .categorySuggestion(template.category())
                        .description(template.description())
                        .externalReference(referencePrefix + "-" + String.format("%03d", sequence++))
                        .build());
            }
        }

        return transactions;
    }

    private List<RecurringSandboxTransactionTemplate> studentTemplates() {
        return List.of(
                template("Salaire Stage", "SALARY DIGITAL LAB TN", 780.0, 15.0, SandboxTransactionType.CREDIT, 2, 9, 15, TransactionCategory.SALAIRE, "Salaire mensuel de stage"),
                template("Loyer Residence", "LOYER RESIDENCE ENNASR", 380.0, 0.0, SandboxTransactionType.DEBIT, 4, 8, 10, TransactionCategory.LOGEMENT, "Paiement mensuel du loyer"),
                template("Monoprix Campus", "MONOPRIX EL MANAR TN", 23.4, 1.1, SandboxTransactionType.DEBIT, 6, 18, 20, TransactionCategory.SUPERMARCHE, "Courses de la semaine"),
                template("MG City", "MG MUTUELLEVILLE TN", 18.9, 0.9, SandboxTransactionType.DEBIT, 8, 19, 5, TransactionCategory.SUPERMARCHE, "Courses express"),
                template("Carrefour Market", "CARREFOUR CITY MARSATN", 27.6, 1.3, SandboxTransactionType.DEBIT, 10, 17, 45, TransactionCategory.SUPERMARCHE, "Achats alimentaires"),
                template("Ooredoo", "OOREDOO RECHARGE APP", 15.0, 0.0, SandboxTransactionType.DEBIT, 12, 21, 0, TransactionCategory.OPERATEURS_TELEPHONIQUES, "Recharge forfait mobile"),
                template("Uber", "UBER TRIP TUNIS", 11.7, 0.6, SandboxTransactionType.DEBIT, 14, 22, 10, TransactionCategory.TRANSPORT, "Trajet Uber de retour"),
                template("Bolt", "BOLT TUNIS RIDE", 8.4, 0.5, SandboxTransactionType.DEBIT, 16, 7, 50, TransactionCategory.TRANSPORT, "Trajet Bolt matinal"),
                template("Foody", "FOODY APP TUNIS", 21.8, 1.0, SandboxTransactionType.DEBIT, 18, 20, 35, TransactionCategory.LIVRAISON, "Commande repas a domicile"),
                template("Netflix", "NETFLIX.COM AMSTERDAM", 29.0, 0.0, SandboxTransactionType.DEBIT, 20, 6, 10, TransactionCategory.TECHNOLOGIE, "Abonnement Netflix mensuel")
        );
    }

    private List<RecurringSandboxTransactionTemplate> familyTemplates() {
        return List.of(
                template("Salaire Couple", "SALARY ATTIJARI HOUSEHOLD", 5_200.0, 40.0, SandboxTransactionType.CREDIT, 1, 8, 30, TransactionCategory.SALAIRE, "Versement salaire mensuel"),
                template("Loyer Famille", "LOYER APPARTEMENT LAC 2", 1_350.0, 0.0, SandboxTransactionType.DEBIT, 3, 8, 5, TransactionCategory.LOGEMENT, "Paiement du loyer familial"),
                template("Carrefour La Marsa", "CARREFOUR LAMARSA TN", 210.6, 8.0, SandboxTransactionType.DEBIT, 5, 19, 40, TransactionCategory.SUPERMARCHE, "Courses mensuelles de la famille"),
                template("Monoprix Mutuelleville", "MONOPRIX MUTUELLEVILLE", 96.3, 4.5, SandboxTransactionType.DEBIT, 7, 18, 25, TransactionCategory.SUPERMARCHE, "Courses hebdomadaires"),
                template("MG Superette", "MG ENNASR TN", 74.8, 3.8, SandboxTransactionType.DEBIT, 9, 18, 50, TransactionCategory.SUPERMARCHE, "Achat produits du quotidien"),
                template("Orange Fibre", "ORANGE FIBRE TUNISIE", 72.0, 0.0, SandboxTransactionType.DEBIT, 11, 9, 20, TransactionCategory.OPERATEURS_TELEPHONIQUES, "Facture internet fibre"),
                template("Ooredoo Mobile", "OOREDOO POSTPAID FAMILY", 58.5, 0.0, SandboxTransactionType.DEBIT, 13, 9, 35, TransactionCategory.OPERATEURS_TELEPHONIQUES, "Facture forfait mobile"),
                template("Pharmacie Centrale", "PHARMACIE CENTRALE TN", 42.9, 2.1, SandboxTransactionType.DEBIT, 15, 13, 10, TransactionCategory.SANTE, "Achats pharmacie familiale"),
                template("Foody Family", "FOODY FAMILY DINNER", 48.4, 2.0, SandboxTransactionType.DEBIT, 17, 20, 45, TransactionCategory.LIVRAISON, "Commande diner du week-end"),
                template("Uber Family", "UBER TRIP LAC2", 17.2, 1.1, SandboxTransactionType.DEBIT, 19, 7, 40, TransactionCategory.TRANSPORT, "Trajet Uber ecole")
        );
    }

    private List<RecurringSandboxTransactionTemplate> salariedTemplates() {
        return List.of(
                template("Salaire Entreprise", "PAYROLL TECH COMPANY TN", 3_850.0, 25.0, SandboxTransactionType.CREDIT, 1, 8, 20, TransactionCategory.SALAIRE, "Salaire mensuel"),
                template("Loyer Appartement", "LOYER MENZAH 6", 920.0, 0.0, SandboxTransactionType.DEBIT, 3, 8, 0, TransactionCategory.LOGEMENT, "Paiement du loyer"),
                template("Carrefour Express", "CARREFOUR EXPRESS CENTRE", 88.5, 4.0, SandboxTransactionType.DEBIT, 5, 19, 5, TransactionCategory.SUPERMARCHE, "Courses du foyer"),
                template("MG Market", "MG CENTRE URBAIN NORD", 46.3, 2.4, SandboxTransactionType.DEBIT, 7, 18, 15, TransactionCategory.SUPERMARCHE, "Courses rapides apres travail"),
                template("Orange Mobile", "ORANGE MOBILE TUNISIE", 39.9, 0.0, SandboxTransactionType.DEBIT, 9, 9, 10, TransactionCategory.OPERATEURS_TELEPHONIQUES, "Forfait mobile Orange"),
                template("Uber", "UBER BUSINESS TUNIS", 14.8, 0.8, SandboxTransactionType.DEBIT, 11, 8, 25, TransactionCategory.TRANSPORT, "Trajet domicile bureau"),
                template("Netflix", "NETFLIX.COM SUBSCRIPTION", 29.0, 0.0, SandboxTransactionType.DEBIT, 13, 6, 5, TransactionCategory.TECHNOLOGIE, "Abonnement streaming"),
                template("Zara Tunis City", "ZARA TUNIS CITY", 124.0, 7.5, SandboxTransactionType.DEBIT, 15, 20, 20, TransactionCategory.SHOPPING, "Achat shopping"),
                template("Pharmacie Lafayette", "PHARMACIE LAFAYETTE TN", 26.7, 1.4, SandboxTransactionType.DEBIT, 17, 13, 30, TransactionCategory.SANTE, "Achats sante"),
                template("Foody Lunch", "FOODY OFFICE LUNCH", 31.6, 1.2, SandboxTransactionType.DEBIT, 19, 12, 50, TransactionCategory.LIVRAISON, "Commande dejeuner bureau")
        );
    }

    private List<RecurringSandboxTransactionTemplate> freelancerTemplates() {
        return List.of(
                template("Virement Client", "CLIENT TRANSFER REMOTE", 2_450.0, 120.0, SandboxTransactionType.CREDIT, 2, 10, 0, TransactionCategory.BANQUE, "Paiement mission freelance"),
                template("Coworking Lac 1", "COWORKING LAC 1 TN", 280.0, 0.0, SandboxTransactionType.DEBIT, 4, 8, 45, TransactionCategory.LOGEMENT, "Abonnement espace coworking"),
                template("Carrefour Market", "CARREFOUR MARKET LAC1", 54.7, 2.7, SandboxTransactionType.DEBIT, 6, 18, 10, TransactionCategory.SUPERMARCHE, "Achats alimentaires"),
                template("MG La Soukra", "MG LASOUKRA TN", 37.8, 1.8, SandboxTransactionType.DEBIT, 8, 18, 55, TransactionCategory.SUPERMARCHE, "Courses quotidiennes"),
                template("Orange Business", "ORANGE BUSINESS MOBILE", 44.0, 0.0, SandboxTransactionType.DEBIT, 10, 9, 15, TransactionCategory.OPERATEURS_TELEPHONIQUES, "Forfait mobile professionnel"),
                template("Uber", "UBER FREELANCE MEETING", 16.4, 1.0, SandboxTransactionType.DEBIT, 12, 9, 40, TransactionCategory.TRANSPORT, "Trajet client Uber"),
                template("Bolt", "BOLT CLIENT VISIT TN", 10.6, 0.6, SandboxTransactionType.DEBIT, 14, 18, 25, TransactionCategory.TRANSPORT, "Trajet client Bolt"),
                template("Foody", "FOODY MIDDAY ORDER", 28.9, 1.1, SandboxTransactionType.DEBIT, 16, 13, 15, TransactionCategory.LIVRAISON, "Repas commande pendant mission"),
                template("Netflix", "NETFLIX.COM AMSTERDAM", 29.0, 0.0, SandboxTransactionType.DEBIT, 18, 6, 15, TransactionCategory.TECHNOLOGIE, "Abonnement Netflix"),
                template("Decathlon Tunis", "DECATHLON TUNIS", 95.0, 6.0, SandboxTransactionType.DEBIT, 20, 20, 5, TransactionCategory.SHOPPING, "Equipement sport")
        );
    }

    private List<RecurringSandboxTransactionTemplate> premiumTemplates() {
        return List.of(
                template("Salaire Management", "PAYROLL EXECUTIVE TN", 9_200.0, 75.0, SandboxTransactionType.CREDIT, 1, 8, 10, TransactionCategory.SALAIRE, "Salaire mensuel management"),
                template("Loyer Premium", "LOYER RESIDENCE GAMMARTH", 2_300.0, 0.0, SandboxTransactionType.DEBIT, 3, 8, 5, TransactionCategory.LOGEMENT, "Paiement residence Gammarth"),
                template("Carrefour Gourmet", "CARREFOUR GOURMET MALL", 185.4, 10.0, SandboxTransactionType.DEBIT, 5, 19, 20, TransactionCategory.SUPERMARCHE, "Courses premium"),
                template("Monoprix Premium", "MONOPRIX GOURMET TN", 122.8, 6.0, SandboxTransactionType.DEBIT, 7, 18, 35, TransactionCategory.SUPERMARCHE, "Epicerie fine"),
                template("Orange Premium", "ORANGE PREMIUM PLAN", 95.0, 0.0, SandboxTransactionType.DEBIT, 9, 9, 45, TransactionCategory.OPERATEURS_TELEPHONIQUES, "Forfait mobile premium"),
                template("Uber Black", "UBER BLACK TUNIS", 38.6, 2.0, SandboxTransactionType.DEBIT, 11, 22, 15, TransactionCategory.TRANSPORT, "Trajet Uber Black"),
                template("Bolt Premium", "BOLT PREMIUM RIDE TN", 25.4, 1.4, SandboxTransactionType.DEBIT, 13, 8, 25, TransactionCategory.TRANSPORT, "Trajet Bolt premium"),
                template("Netflix Premium", "NETFLIX PREMIUM PLAN", 45.0, 0.0, SandboxTransactionType.DEBIT, 15, 6, 20, TransactionCategory.TECHNOLOGIE, "Abonnement Netflix premium"),
                template("Mall of Sousse", "MALL OF SOUSSE SHOPPING", 420.0, 22.0, SandboxTransactionType.DEBIT, 17, 20, 30, TransactionCategory.SHOPPING, "Session shopping premium"),
                template("Pharmacie Lac 2", "PHARMACIE LAC2 PREMIUM", 58.3, 3.0, SandboxTransactionType.DEBIT, 19, 13, 20, TransactionCategory.SANTE, "Achat pharmacie et bien-etre")
        );
    }

    private RecurringSandboxTransactionTemplate template(
            String merchantName,
            String rawLabel,
            double baseAmount,
            double amountStep,
            SandboxTransactionType transactionType,
            int daysAgo,
            int hour,
            int minute,
            TransactionCategory category,
            String description) {
        return new RecurringSandboxTransactionTemplate(
                merchantName,
                rawLabel,
                baseAmount,
                amountStep,
                transactionType,
                daysAgo,
                hour,
                minute,
                category,
                description
        );
    }

    private record SandboxCardSeed(
            String holderName,
            String cardNumber,
            int expiryMonth,
            int expiryYear,
            String cvv,
            CardType cardType,
            String bankName,
            CardStatus status,
            double initialBalance,
            String referencePrefix,
            List<RecurringSandboxTransactionTemplate> templates) {
    }

    private record RecurringSandboxTransactionTemplate(
            String merchantName,
            String rawLabel,
            double baseAmount,
            double amountStep,
            SandboxTransactionType transactionType,
            int daysAgo,
            int hour,
            int minute,
            TransactionCategory category,
            String description) {
    }
}



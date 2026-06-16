package com.adem.attijari_compass.config;

import com.adem.attijari_compass.entity.CardCatalog;
import com.adem.attijari_compass.entity.CardScope;
import com.adem.attijari_compass.repository.CardCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE - 100)
@Slf4j
public class AttijariCardCatalogInitializer implements CommandLineRunner {

    private final CardCatalogRepository cardCatalogRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<CardCatalogSeed> seeds = List.of(
                card(
                        "CARTE_FLEX",
                        "Carte Flex",
                        "Carte de paiement flexible avec option de fractionnement pour les achats du quotidien.",
                        "Visa",
                        CardScope.CREDIT,
                        "12000.00",
                        "3000.00",
                        true,
                        true,
                        true,
                        12,
                        true,
                        null
                ),
                card(
                        "CARTE_PLATINUM",
                        "Carte Platinum",
                        "Carte premium avec plafonds eleves et acceptation internationale pour les clients haut de gamme.",
                        "Mastercard",
                        CardScope.CREDIT,
                        "30000.00",
                        "8000.00",
                        true,
                        true,
                        false,
                        null,
                        true,
                        null
                ),
                card(
                        "CARTE_GOLD_NATIONALE",
                        "Carte Gold Nationale",
                        "Carte Gold reservee au reseau national avec plafonds confortables pour paiements et retraits en Tunisie.",
                        "Mastercard",
                        CardScope.DEBIT,
                        "15000.00",
                        "4000.00",
                        true,
                        false,
                        false,
                        null,
                        true,
                        null
                ),
                card(
                        "CARTE_GOLD_INTERNATIONALE",
                        "Carte Gold Internationale",
                        "Carte Gold avec usage local et international pour paiements en ligne, voyages et retraits a l'etranger.",
                        "Mastercard",
                        CardScope.DEBIT,
                        "18000.00",
                        "4500.00",
                        true,
                        true,
                        false,
                        null,
                        true,
                        null
                ),
                card(
                        "CARTE_VISA_NATIONALE",
                        "Carte Visa Nationale",
                        "Carte Visa pour retraits et paiements sur le territoire national avec enveloppe adaptee au quotidien.",
                        "Visa",
                        CardScope.DEBIT,
                        "8000.00",
                        "2000.00",
                        true,
                        false,
                        false,
                        null,
                        true,
                        null
                ),
                card(
                        "CARTE_VISA_INTERNATIONALE",
                        "Carte Visa Internationale",
                        "Carte Visa polyvalente permettant les paiements nationaux et internationaux en magasin comme en ligne.",
                        "Visa",
                        CardScope.DEBIT,
                        "10000.00",
                        "2500.00",
                        true,
                        true,
                        false,
                        null,
                        true,
                        null
                ),
                card(
                        "CARTE_CIB",
                        "Carte CIB",
                        "Carte interbancaire nationale pour retraits GAB et paiements domestiques avec parametrage simple.",
                        "CIB",
                        CardScope.DEBIT,
                        "5000.00",
                        "1500.00",
                        false,
                        false,
                        false,
                        null,
                        true,
                        null
                ),
                card(
                        "CARTE_TAWA_TAWA",
                        "Carte TAWA TAWA",
                        "Carte a budget maitrise pour jeunes clients, adaptee aux achats courants et au suivi des depenses.",
                        "Visa",
                        CardScope.PREPAID,
                        "2500.00",
                        "500.00",
                        true,
                        false,
                        false,
                        null,
                        true,
                        null
                ),
                card(
                        "CARTE_IDDIKHAR",
                        "Carte Iddikhar",
                        "Carte d'epargne orientee gestion prudente avec plafonds moderes pour les usages essentiels.",
                        "Visa",
                        CardScope.DEBIT,
                        "4000.00",
                        "1000.00",
                        false,
                        false,
                        false,
                        null,
                        true,
                        null
                ),
                card(
                        "CARTE_VOYAGE",
                        "Carte Voyage",
                        "Carte dediee aux deplacements et a l'allocation touristique avec paiements et retraits a l'international.",
                        "Visa",
                        CardScope.PREPAID,
                        "15000.00",
                        "3500.00",
                        true,
                        true,
                        false,
                        null,
                        true,
                        null
                ),
                card(
                        "CARTE_OULIDHA",
                        "Carte Oulidha",
                        "Carte d'initiation destinee aux plus jeunes avec controle des depenses et plafonds renforces.",
                        "Visa",
                        CardScope.PREPAID,
                        "2000.00",
                        "400.00",
                        false,
                        false,
                        false,
                        null,
                        true,
                        null
                ),
                card(
                        "CARTE_TECHNOLOGIQUE",
                        "Carte Technologique",
                        "Carte specialisee dans le paiement internet international pour logiciels, abonnements et services digitaux.",
                        "Visa",
                        CardScope.PREPAID,
                        "7000.00",
                        "0.00",
                        true,
                        true,
                        false,
                        null,
                        true,
                        null
                ),
                card(
                        "CARTE_AVENIR",
                        "Carte Avenir",
                        "Carte pour etudiants et jeunes actifs avec paiements du quotidien, e-commerce local et retraits moderes.",
                        "Visa",
                        CardScope.DEBIT,
                        "3000.00",
                        "800.00",
                        true,
                        false,
                        false,
                        null,
                        true,
                        null
                )
        );

        int inserted = 0;
        int updated = 0;

        for (CardCatalogSeed seed : seeds) {
            boolean created = upsert(seed);
            if (created) {
                inserted++;
            } else {
                updated++;
            }
        }

        log.info("Attijari card catalog ready: {} inserted, {} updated", inserted, updated);
    }

    private boolean upsert(CardCatalogSeed seed) {
        CardCatalog cardCatalog = cardCatalogRepository.findByCode(seed.code()).orElseGet(CardCatalog::new);
        boolean created = cardCatalog.getId() == null;

        cardCatalog.setCode(seed.code());
        cardCatalog.setName(seed.name());
        cardCatalog.setDescription(seed.description());
        cardCatalog.setBrand(seed.brand());
        cardCatalog.setScope(seed.scope());
        cardCatalog.setMaxPaymentLimit(seed.maxPaymentLimit());
        cardCatalog.setMaxWithdrawalLimit(seed.maxWithdrawalLimit());
        cardCatalog.setAllowsOnlinePayment(seed.allowsOnlinePayment());
        cardCatalog.setAllowsInternationalPayment(seed.allowsInternationalPayment());
        cardCatalog.setAllowsInstallments(seed.allowsInstallments());
        cardCatalog.setInstallmentMonthsMax(seed.installmentMonthsMax());
        cardCatalog.setActive(seed.active());
        cardCatalog.setImageUrl(seed.imageUrl());

        cardCatalogRepository.save(cardCatalog);
        return created;
    }

    private CardCatalogSeed card(
            String code,
            String name,
            String description,
            String brand,
            CardScope scope,
            String maxPaymentLimit,
            String maxWithdrawalLimit,
            boolean allowsOnlinePayment,
            boolean allowsInternationalPayment,
            boolean allowsInstallments,
            Integer installmentMonthsMax,
            boolean active,
            String imageUrl) {
        return new CardCatalogSeed(
                code,
                name,
                description,
                brand,
                scope,
                new BigDecimal(maxPaymentLimit),
                new BigDecimal(maxWithdrawalLimit),
                allowsOnlinePayment,
                allowsInternationalPayment,
                allowsInstallments,
                installmentMonthsMax,
                active,
                imageUrl
        );
    }

    private record CardCatalogSeed(
            String code,
            String name,
            String description,
            String brand,
            CardScope scope,
            BigDecimal maxPaymentLimit,
            BigDecimal maxWithdrawalLimit,
            boolean allowsOnlinePayment,
            boolean allowsInternationalPayment,
            boolean allowsInstallments,
            Integer installmentMonthsMax,
            boolean active,
            String imageUrl) {
    }
}

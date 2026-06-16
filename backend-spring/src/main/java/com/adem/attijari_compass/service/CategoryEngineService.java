package com.adem.attijari_compass.service;

import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.model.categorization.CategorizationResult;
import com.adem.attijari_compass.model.categorization.CategorizationSources;
import com.adem.attijari_compass.util.TransactionTextNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
public class CategoryEngineService {

    private static final double EXACT_MERCHANT_CONFIDENCE = 0.99d;
    private static final double NEAR_EXACT_MERCHANT_CONFIDENCE = 0.97d;
    private static final double FUZZY_MERCHANT_THRESHOLD = 0.91d;
    private static final double FUZZY_MERCHANT_MARGIN = 0.04d;
    private static final double KEYWORD_MIN_SCORE = 1.80d;
    private static final double KEYWORD_MAX_CONFIDENCE = 0.92d;

    private static final Pattern COMPACT_PATTERN = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}\\u0600-\\u06FF]");

    /*
     * This structure intentionally mirrors what can later come from a MerchantKnowledge table:
     * canonical merchant + category + aliases + confidence strategy.
     */
    private static final List<MerchantRule> MERCHANT_RULES = List.of(
            merchantRule(TransactionCategory.SUPERMARCHE, "carrefour", "carrefour market", "carrefour express"),
            merchantRule(TransactionCategory.SUPERMARCHE, "monoprix", "monop", "monoprix tunis"),
            merchantRule(TransactionCategory.SUPERMARCHE, "auchan"),
            merchantRule(TransactionCategory.SUPERMARCHE, "aziza", "aziza market"),
            merchantRule(TransactionCategory.SUPERMARCHE, "geant", "geant tunis", "geant casino"),
            merchantRule(TransactionCategory.SUPERMARCHE, "bim", "bim market"),
            merchantRule(TransactionCategory.SUPERMARCHE, "mg", "magasin general", "magasin generale", "mg city"),
            merchantRule(TransactionCategory.STATION_SERVICES, "bp", "bp fuel", "shell", "total", "totalenergies", "ola energy", "agil", "ajil"),
            merchantRule(TransactionCategory.LIVRAISON, "foody", "foudy"),
            merchantRule(TransactionCategory.LIVRAISON, "glovo"),
            merchantRule(TransactionCategory.LIVRAISON, "talabat"),
            merchantRule(TransactionCategory.TRANSPORT, "uber", "uber trip"),
            merchantRule(TransactionCategory.TRANSPORT, "bolt"),
            merchantRule(TransactionCategory.TRANSPORT, "yango"),
            merchantRule(TransactionCategory.TRANSPORT, "indrive", "in drive"),
            merchantRule(TransactionCategory.SERVICE_AUTO, "garage", "midas", "point s", "speedy", "car wash", "lavage auto"),
            merchantRule(TransactionCategory.OPERATEURS_TELEPHONIQUES, "ooredoo", "oredoo", "ooredoo tunisie"),
            merchantRule(TransactionCategory.OPERATEURS_TELEPHONIQUES, "orange", "orange tunisie"),
            merchantRule(TransactionCategory.OPERATEURS_TELEPHONIQUES, "topnet"),
            merchantRule(TransactionCategory.OPERATEURS_TELEPHONIQUES, "tunisie telecom", "telecom"),
            merchantRule(TransactionCategory.STEG_SONEDE, "steg"),
            merchantRule(TransactionCategory.STEG_SONEDE, "sonede"),
            merchantRule(TransactionCategory.DIVERTISSEMENT, "netflix"),
            merchantRule(TransactionCategory.DIVERTISSEMENT, "spotify"),
            merchantRule(TransactionCategory.DIVERTISSEMENT, "deezer"),
            merchantRule(TransactionCategory.DIVERTISSEMENT, "anghami"),
            merchantRule(TransactionCategory.DIVERTISSEMENT, "youtube premium"),
            merchantRule(TransactionCategory.DIVERTISSEMENT, "disney", "disney plus"),
            merchantRule(TransactionCategory.DIVERTISSEMENT, "prime video"),
            merchantRule(TransactionCategory.TECHNOLOGIE, "google one"),
            merchantRule(TransactionCategory.TECHNOLOGIE, "icloud"),
            merchantRule(TransactionCategory.EDUCATION, "esprit", "esprit school of engineering"),
            merchantRule(TransactionCategory.EDUCATION, "universite centrale", "university centrale"),
            merchantRule(TransactionCategory.EDUCATION, "sesame"),
            merchantRule(TransactionCategory.EDUCATION, "tek up", "tekup"),
            merchantRule(TransactionCategory.EDUCATION, "udemy"),
            merchantRule(TransactionCategory.EDUCATION, "coursera"),
            merchantRule(TransactionCategory.SHOPPING, "zara"),
            merchantRule(TransactionCategory.SHOPPING, "h m", "hm"),
            merchantRule(TransactionCategory.SHOPPING, "lc waikiki"),
            merchantRule(TransactionCategory.SHOPPING, "decathlon"),
            merchantRule(TransactionCategory.SHOPPING, "amazon"),
            merchantRule(TransactionCategory.SHOPPING, "jumia"),
            merchantRule(TransactionCategory.BEAUTE, "barber", "coiffeur", "coiffure", "coif", "salon", "beauty", "spa"),
            merchantRule(TransactionCategory.SANTE, "gym", "fitness", "salle sport", "salle de sport"),
            merchantRule(TransactionCategory.NETTOYAGE, "pressing", "dry clean", "laundry", "blanchisserie"),
            merchantRule(TransactionCategory.IMPORT_EXPORT, "dhl", "fedex", "ups", "aramex", "cargo", "freight"),
            merchantRule(TransactionCategory.CAFES, "starbucks"),
            merchantRule(TransactionCategory.RESTAURANT, "restaurant", "resto", "kfc", "burger king", "mcdonald", "mcdonalds", "mcdo", "pizza hut"),
            merchantRule(TransactionCategory.HOTEL, "airbnb"),
            merchantRule(TransactionCategory.HOTEL, "booking", "booking com"),
            merchantRule(TransactionCategory.BANQUE, "western union"),
            merchantRule(TransactionCategory.BANQUE, "moneygram"),
            merchantRule(TransactionCategory.BANQUE, "d17"),
            merchantRule(TransactionCategory.BANQUE, "flouci")
    );

    private static final List<PriorityRule> PRIORITY_RULES = List.of(
            priorityRule(
                    TransactionCategory.SALAIRE,
                    0.99d,
                    "priority:salaire_income",
                    keywords(),
                    keywords("salaire", "salary", "paie", "payroll", "fiche paie", "virement salaire"),
                    List.of(),
                    keywords()
            ),
            priorityRule(
                    TransactionCategory.SERVICE_AUTO,
                    0.99d,
                    "priority:service_auto_assurance",
                    keywords(),
                    keywords(),
                    List.of(
                            keywords("assurance"),
                            keywords("voiture", "auto", "vehicule")
                    ),
                    keywords()
            ),
            priorityRule(
                    TransactionCategory.DIVERTISSEMENT,
                    0.98d,
                    "priority:divertissement_subscription",
                    keywords("netflix", "spotify", "youtube premium", "disney", "disney plus", "prime video"),
                    keywords("netflix", "spotify", "youtube premium", "cinema", "game", "gaming", "playstation", "xbox", "disney", "disney plus", "prime video"),
                    List.of(),
                    keywords()
            ),
            priorityRule(
                    TransactionCategory.RESTAURANT,
                    0.98d,
                    "priority:restaurant_food",
                    keywords("mcdo", "mcdonald", "mcdonalds", "kfc"),
                    keywords("restaurant", "resto", "terrasse", "pizza", "burger", "tacos", "sushi", "kfc", "mcdo", "mcdonald", "mcdonalds", "food delivery", "fast food"),
                    List.of(),
                    keywords("supermarche", "market")
            ),
            priorityRule(
                    TransactionCategory.CAFES,
                    0.97d,
                    "priority:cafes_beverages",
                    keywords("starbucks"),
                    keywords("cafe", "coffee", "espresso", "latte", "cappuccino", "starbucks"),
                    List.of(),
                    keywords()
            ),
            priorityRule(
                    TransactionCategory.SUPERMARCHE,
                    0.98d,
                    "priority:supermarche_alias",
                    keywords("carrefour", "monoprix", "auchan", "mg", "geant", "aziza"),
                    keywords("supermarche", "epicerie"),
                    List.of(),
                    keywords("fast food", "restaurant")
            ),
            priorityRule(
                    TransactionCategory.ALIMENTATION,
                    0.96d,
                    "priority:alimentation_food",
                    keywords(),
                    keywords("boulangerie", "patisserie", "viande", "fruits", "legumes", "alimentation", "produits alimentaires"),
                    List.of(),
                    keywords("restaurant", "mcdo", "supermarche")
            ),
            priorityRule(
                    TransactionCategory.STATION_SERVICES,
                    0.98d,
                    "priority:station_services_fuel",
                    keywords("bp", "shell", "total", "ola energy"),
                    keywords("essence", "station", "bp", "shell", "total", "ola energy", "carburant", "diesel", "gasoil"),
                    List.of(),
                    keywords("restaurant", "cafe")
            ),
            priorityRule(
                    TransactionCategory.OPERATEURS_TELEPHONIQUES,
                    0.97d,
                    "priority:telecom_operator",
                    keywords("ooredoo", "orange", "tunisie telecom"),
                    keywords("ooredoo", "orange", "tunisie telecom", "telecom", "forfait", "mobile", "internet mobile", "recharge"),
                    List.of(),
                    keywords()
            ),
            priorityRule(
                    TransactionCategory.STEG_SONEDE,
                    0.97d,
                    "priority:steg_sonede_utility",
                    keywords("steg", "sonede"),
                    keywords("facture eau", "facture electricite"),
                    List.of(
                            keywords("facture", "invoice", "bill", "utilities", "utility"),
                            keywords("eau", "electricite")
                    ),
                    keywords("kiosque", "restaurant")
            ),
            priorityRule(
                    TransactionCategory.LOGEMENT,
                    0.97d,
                    "priority:logement_rent",
                    keywords(),
                    keywords("loyer", "rent", "appartement", "maison", "syndic", "location logement"),
                    List.of(),
                    keywords()
            ),
            priorityRule(
                    TransactionCategory.SANTE,
                    0.97d,
                    "priority:sante_medical",
                    keywords(),
                    keywords("pharmacie", "medecin", "clinique", "hopital", "consultation", "analyse medicale", "dentaire", "medical"),
                    List.of(),
                    keywords()
            ),
            priorityRule(
                    TransactionCategory.SANTE,
                    0.97d,
                    "priority:sante_sport",
                    keywords("gym", "fitness"),
                    keywords("sport", "salle sport", "salle de sport", "gym", "fitness", "musculation", "coach sportif", "abonnement sport"),
                    List.of(),
                    keywords("shopping", "restaurant")
            ),
            priorityRule(
                    TransactionCategory.BANQUE,
                    0.96d,
                    "priority:banque_fees",
                    keywords(),
                    keywords("frais bancaire", "commission bancaire", "tenue de compte", "agios", "carte bancaire", "frais compte"),
                    List.of(),
                    keywords("assurance voiture", "salaire")
            ),
            priorityRule(
                    TransactionCategory.EPARGNE,
                    0.96d,
                    "priority:epargne_transfer",
                    keywords(),
                    keywords("epargne", "saving", "economies", "virement epargne", "compte epargne", "placement"),
                    List.of(),
                    keywords()
            ),
            priorityRule(
                    TransactionCategory.BEAUTE,
                    0.96d,
                    "priority:beaute_services",
                    keywords("coif", "coiffeur"),
                    keywords("coif", "coiffure", "coiffeur", "salon", "beaute", "spa", "maquillage", "cosmetique"),
                    List.of(),
                    keywords()
            ),
            priorityRule(
                    TransactionCategory.TECHNOLOGIE,
                    0.95d,
                    "priority:technologie_devices",
                    keywords(),
                    keywords("ordinateur", "pc", "laptop", "telephone", "smartphone", "accessoire tech", "informatique", "software", "materiel informatique"),
                    List.of(),
                    keywords("netflix", "spotify", "prime video", "disney")
            ),
            priorityRule(
                    TransactionCategory.SHOPPING,
                    0.95d,
                    "priority:shopping_fashion",
                    keywords("zara", "pull and bear", "bershka"),
                    keywords("shopping", "vetement", "chaussure", "mode", "zara", "pull and bear", "bershka", "mall"),
                    List.of(),
                    keywords("sport", "gym", "restaurant", "netflix", "assurance voiture")
            ),
            priorityRule(
                    TransactionCategory.TRANSPORT,
                    0.95d,
                    "priority:transport_mobility",
                    keywords("bolt", "uber"),
                    keywords("taxi", "bolt", "uber", "bus", "metro", "train", "transport"),
                    List.of(),
                    keywords()
            ),
            priorityRule(
                    TransactionCategory.HOTEL,
                    0.96d,
                    "priority:hotel_stay",
                    keywords("hotel", "airbnb", "booking"),
                    keywords("hotel", "hebergement", "reservation hotel"),
                    List.of(),
                    keywords("flight", "vol")
            ),
            priorityRule(
                    TransactionCategory.VOYAGE,
                    0.95d,
                    "priority:voyage_trip",
                    keywords(),
                    keywords("vol", "flight", "trip", "voyage", "travel"),
                    List.of(),
                    keywords("reservation hotel", "hebergement")
            ),
            priorityRule(
                    TransactionCategory.LIVRAISON,
                    0.95d,
                    "priority:livraison_delivery",
                    keywords("glovo", "talabat", "foody"),
                    keywords("livraison", "delivery", "courrier", "colis", "express"),
                    List.of(),
                    keywords()
            ),
            priorityRule(
                    TransactionCategory.IMPORT_EXPORT,
                    0.95d,
                    "priority:import_export_trade",
                    keywords("dhl", "fedex", "ups", "aramex"),
                    keywords("import", "export", "douane", "fret", "shipping international"),
                    List.of(),
                    keywords()
            ),
            priorityRule(
                    TransactionCategory.NETTOYAGE,
                    0.94d,
                    "priority:nettoyage_cleaning",
                    keywords(),
                    keywords("nettoyage", "cleaning", "menage", "lavage"),
                    List.of(),
                    keywords()
            ),
            priorityRule(
                    TransactionCategory.DISTRIBUTION,
                    0.93d,
                    "priority:distribution_logistics",
                    keywords(),
                    keywords("distribution", "distributeur", "logistique commerciale", "grossiste"),
                    List.of(),
                    keywords()
            )
    );

    private static final List<ContextRule> CONTEXT_RULES = List.of(
            contextRule(
                    TransactionCategory.STATION_SERVICES,
                    0.95d,
                    keywords("ajil", "agil", "ola energy", "total", "totalenergies", "shell", "bp"),
                    keywords("essence", "carburant", "fuel", "diesel", "gasoil", "station service",
                            "\u0628\u0646\u0632\u064A\u0646", "\u0648\u0642\u0648\u062F"),
                    keywords("kiosque", "cafe", "coffee", "sandwich")
            ),
            contextRule(
                    TransactionCategory.ALIMENTATION,
                    0.93d,
                    keywords("ajil", "agil", "ola energy", "total", "totalenergies", "shell"),
                    keywords("kiosque", "epicerie", "eau", "snack sale", "chips", "biscuit",
                            "\u0643\u064A\u0648\u0633\u0643", "\u0628\u0642\u0627\u0644\u0629"),
                    keywords("essence", "carburant", "fuel", "diesel", "cafe", "coffee")
            ),
            contextRule(
                    TransactionCategory.RESTAURANT,
                    0.94d,
                    keywords("restaurant", "resto", "fast food", "kfc", "burger king", "mcdonald", "pizza hut"),
                    keywords("pizza", "burger", "shawarma", "sushi", "diner", "dejeuner",
                            "\u0645\u0637\u0639\u0645", "\u0648\u062C\u0628\u0629"),
                    keywords("essence", "carburant", "fuel", "kiosque")
            ),
            contextRule(
                    TransactionCategory.CAFES,
                    0.94d,
                    keywords("ajil", "agil", "ola energy", "total", "totalenergies", "shell"),
                    keywords("cafe", "coffee", "espresso", "tea", "latte",
                            "\u0642\u0647\u0648\u0629"),
                    keywords("essence", "carburant", "fuel", "kiosque")
            ),
            contextRule(
                    TransactionCategory.SERVICE_AUTO,
                    0.95d,
                    keywords("garage", "midas", "point s", "speedy", "car wash", "lavage auto", "atelier auto"),
                    keywords("vidange", "pneu", "pneus", "reparation", "entretien", "mechanic", "car wash", "lavage"),
                    keywords("fuel", "restaurant", "coffee")
            ),
            contextRule(
                    TransactionCategory.BEAUTE,
                    0.94d,
                    keywords("barber", "coiffeur", "salon", "beauty", "spa"),
                    keywords("haircut", "coiffure", "beard", "manicure", "pedicure", "soin visage"),
                    keywords("fuel", "restaurant", "garage")
            ),
            contextRule(
                    TransactionCategory.NETTOYAGE,
                    0.94d,
                    keywords("pressing", "laundry", "dry clean", "blanchisserie", "clean"),
                    keywords("nettoyage", "lavage", "pressing", "menage", "repassage"),
                    keywords("fuel", "restaurant", "beauty")
            ),
            contextRule(
                    TransactionCategory.EDUCATION,
                    0.95d,
                    keywords("esprit", "sesame", "universite", "school", "ecole"),
                    keywords("inscription", "tuition", "formation", "school fee", "frais scolaire",
                            "cours", "course", "training", "\u062A\u0639\u0644\u064A\u0645", "\u0645\u062F\u0631\u0633\u0629"),
                    keywords("restaurant", "grocery", "fuel")
            ),
            contextRule(
                    TransactionCategory.SHOPPING,
                    0.94d,
                    keywords("zara", "bershka", "pull and bear", "mall", "boutique"),
                    keywords("vetement", "mode", "chaussure", "shopping", "fashion", "clothing"),
                    keywords("sport", "fitness", "restaurant", "fuel")
            )
    );

    private static final List<KeywordRuleSet> KEYWORD_RULES = List.of(
            keywordRule(
                    TransactionCategory.SUPERMARCHE,
                    keywords("supermarche", "supermarket", "carrefour", "monoprix", "aziza", "geant", "bim",
                            "mg", "magasin general"),
                    keywords("courses", "grandes courses", "market"),
                    keywords("fuel", "restaurant", "barber"),
                    keywords("supermarche", "supermarket", "carrefour", "monoprix")
            ),
            keywordRule(
                    TransactionCategory.ALIMENTATION,
                    keywords("supermarche", "supermarket", "epicerie", "grocery", "groceries", "souk",
                            "market", "carrefour", "monoprix", "aziza", "geant", "bim",
                            "\u0633\u0648\u0642", "\u0628\u0642\u0627\u0644\u0629", "\u0645\u0648\u0627\u062F \u063A\u0630\u0627\u0626\u064A\u0629"),
                    keywords("courses", "fruits", "legumes", "bread", "boulangerie", "kiosque", "mini market"),
                    keywords("essence", "carburant", "fuel", "taxi", "barber"),
                    keywords("market", "kiosque")
            ),
            keywordRule(
                    TransactionCategory.LIVRAISON,
                    keywords("foody", "talabat", "glovo", "delivery", "livraison", "commande", "order"),
                    keywords("repas a domicile", "dejeuner livre", "diner livre"),
                    keywords("fuel", "salary", "bill"),
                    keywords("delivery", "livraison", "commande")
            ),
            keywordRule(
                    TransactionCategory.RESTAURANT,
                    keywords("restaurant", "resto", "pizza", "burger", "sandwich", "shawarma", "sushi",
                            "brasserie", "diner", "dejeuner", "fast food",
                            "\u0645\u0637\u0639\u0645", "\u0648\u062C\u0628\u0629"),
                    keywords("snack", "brunch", "dessert", "patisserie"),
                    keywords("internet", "facture", "fuel", "essence"),
                    keywords("restaurant", "resto", "pizza")
            ),
            keywordRule(
                    TransactionCategory.CAFES,
                    keywords("coffee", "cafe", "espresso", "cappuccino", "latte", "tea",
                            "\u0642\u0647\u0648\u0629"),
                    keywords("snack", "brunch", "dessert", "patisserie"),
                    keywords("internet", "facture", "fuel", "essence"),
                    keywords("cafe", "coffee", "snack")
            ),
            keywordRule(
                    TransactionCategory.TRANSPORT,
                    keywords("uber", "bolt", "yango", "indrive", "taxi", "parking", "peage",
                            "\u0633\u064A\u0627\u0631\u0629"),
                    keywords("bus", "train", "metro", "tram", "autoroute"),
                    keywords("restaurant", "kiosque", "epicerie"),
                    keywords("bus", "train")
            ),
            keywordRule(
                    TransactionCategory.STATION_SERVICES,
                    keywords("fuel", "essence", "carburant", "diesel", "gasoil", "station service",
                            "shell", "total", "totalenergies", "ola energy", "agil", "ajil", "bp",
                            "\u0628\u0646\u0632\u064A\u0646", "\u0648\u0642\u0648\u062F"),
                    keywords("plein", "pompe", "service station"),
                    keywords("restaurant", "shopping", "salary"),
                    keywords("fuel", "essence", "station service")
            ),
            keywordRule(
                    TransactionCategory.SERVICE_AUTO,
                    keywords("garage", "vidange", "pneu", "pneus", "car wash", "lavage auto", "atelier",
                            "mechanic", "reparation auto", "entretien auto"),
                    keywords("frein", "huile", "revision", "alignement"),
                    keywords("restaurant", "bill", "salary"),
                    keywords("garage", "vidange", "car wash")
            ),
            keywordRule(
                    TransactionCategory.BEAUTE,
                    keywords("beauty", "beaute", "barber", "coiffeur", "salon", "spa"),
                    keywords("haircut", "coiffure", "manicure", "pedicure", "parfum", "cosmetic"),
                    keywords("fuel", "garage", "restaurant"),
                    keywords("beauty", "barber", "spa")
            ),
            keywordRule(
                    TransactionCategory.NETTOYAGE,
                    keywords("nettoyage", "pressing", "laundry", "dry clean", "blanchisserie", "menage"),
                    keywords("repassage", "lavage"),
                    keywords("fuel", "restaurant", "beauty"),
                    keywords("pressing", "laundry", "nettoyage")
            ),
            keywordRule(
                    TransactionCategory.IMPORT_EXPORT,
                    keywords("import", "export", "douane", "freight", "shipping", "cargo", "container"),
                    keywords("transit", "customs"),
                    keywords("restaurant", "fuel", "salary"),
                    keywords("import", "export", "cargo")
            ),
            keywordRule(
                    TransactionCategory.DISTRIBUTION,
                    keywords("distribution", "distributeur", "grossiste", "wholesale", "fournisseur"),
                    keywords("supply", "stock"),
                    keywords("restaurant", "fuel", "salary"),
                    keywords("distribution", "grossiste")
            ),
            keywordRule(
                    TransactionCategory.FACTURES,
                    keywords("facture", "factures", "invoice", "bill", "bills", "utilities", "utility"),
                    keywords("quittance", "echeance", "paiement facture"),
                    keywords("restaurant", "shopping", "salary"),
                    keywords("invoice", "bill", "facture")
            ),
            keywordRule(
                    TransactionCategory.OPERATEURS_TELEPHONIQUES,
                    keywords("facture", "invoice", "bill", "internet", "wifi", "mobile",
                            "telephone", "recharge", "topnet", "ooredoo", "orange", "telecom",
                            "\u0641\u0627\u062A\u0648\u0631\u0629", "\u0627\u0646\u062A\u0631\u0646\u062A", "\u0627\u0648\u0631\u064A\u062F\u0648"),
                    keywords("fibre", "adsl", "data", "telecom"),
                    keywords("restaurant", "supermarche", "salary"),
                    keywords("bill", "telecom")
            ),
            keywordRule(
                    TransactionCategory.STEG_SONEDE,
                    keywords("electricity", "water", "gas", "steg", "sonede", "electricite", "eau", "gaz"),
                    keywords("utility", "utilities", "consommation"),
                    keywords("restaurant", "shopping", "salary"),
                    keywords("steg", "sonede")
            ),
            keywordRule(
                    TransactionCategory.TECHNOLOGIE,
                    keywords("ordinateur", "pc", "laptop", "telephone", "smartphone", "accessoire tech",
                            "informatique", "software", "materiel informatique", "google one", "icloud"),
                    keywords("tech", "digital", "application professionnelle"),
                    keywords("fuel", "restaurant", "salary", "netflix", "spotify", "deezer", "anghami",
                            "youtube premium", "prime video", "disney", "cinema"),
                    keywords("tech", "digital")
            ),
            keywordRule(
                    TransactionCategory.LOGEMENT,
                    keywords("loyer", "rent", "residence", "syndic",
                            "apartment", "housing", "\u0643\u0631\u0627\u0621", "\u0645\u0646\u0632\u0644"),
                    keywords("immobilier"),
                    keywords("restaurant", "fuel"),
                    keywords("rent", "housing")
            ),
            keywordRule(
                    TransactionCategory.HOTEL,
                    keywords("airbnb", "booking", "hotel", "guest house", "hostel"),
                    keywords("resort", "sejour", "hebergement"),
                    keywords("restaurant", "fuel"),
                    keywords("hotel", "booking")
            ),
            keywordRule(
                    TransactionCategory.VOYAGE,
                    keywords("voyage", "travel", "flight", "airline", "airport", "vacance", "vacances"),
                    keywords("ticket avion", "reservation vol", "trip"),
                    keywords("restaurant", "fuel", "salary"),
                    keywords("voyage", "travel", "flight")
            ),
            keywordRule(
                    TransactionCategory.SANTE,
                    keywords("pharmacie", "pharmacy", "clinic", "clinique", "hospital", "hopital", "doctor",
                            "medecin", "dentiste", "medical", "laboratoire",
                            "sport", "salle sport", "salle de sport", "gym", "fitness", "musculation", "coach sportif",
                            "\u0635\u064A\u062F\u0644\u064A\u0629", "\u062F\u0648\u0627\u0621", "\u0639\u0644\u0627\u062C"),
                    keywords("consultation", "analyse", "ordonnance", "abonnement sport"),
                    keywords("restaurant", "fuel", "shopping"),
                    keywords("medical")
            ),
            keywordRule(
                    TransactionCategory.DIVERTISSEMENT,
                    keywords("cinema", "concert", "museum", "musee", "ticket", "playstation", "xbox", "steam",
                            "game", "gaming", "netflix", "spotify", "deezer", "anghami", "youtube premium",
                            "prime video", "disney", "parc", "\u062A\u0630\u0643\u0631\u0629", "\u0645\u062A\u062D\u0641"),
                    keywords("club", "event", "festival", "abonnement streaming"),
                    keywords("salary", "bill", "fuel"),
                    keywords("ticket", "club")
            ),
            keywordRule(
                    TransactionCategory.SHOPPING,
                    keywords("shopping", "mall", "store", "boutique", "fashion", "clothing", "zara", "hm",
                            "decathlon", "amazon", "jumia", "\u0645\u0644\u0627\u0628\u0633", "\u0628\u0648\u062A\u064A\u0643"),
                    keywords("chaussures", "shoes", "parfum"),
                    keywords("salary", "bill", "fuel"),
                    keywords("store", "mall")
            ),
            keywordRule(
                    TransactionCategory.EDUCATION,
                    keywords("esprit", "university", "universite", "school", "ecole", "formation", "training",
                            "course", "cours", "tuition", "udemy", "coursera", "book", "librairie",
                            "\u0645\u062F\u0631\u0633\u0629", "\u062A\u0639\u0644\u064A\u0645"),
                    keywords("inscription", "frais scolaire", "certification"),
                    keywords("restaurant", "fuel", "shopping"),
                    keywords("book", "course")
            ),
            keywordRule(
                    TransactionCategory.SALAIRE,
                    keywords("salary", "salaire", "payroll", "bonus", "prime", "wage",
                            "\u0631\u0627\u062A\u0628", "\u0627\u062C\u0631", "\u0623\u062C\u0631"),
                    keywords("pay", "versement"),
                    keywords("bill", "fuel", "shopping"),
                    keywords("pay")
            ),
            keywordRule(
                    TransactionCategory.EPARGNE,
                    keywords("epargne", "saving", "savings", "investment", "investissement", "fund",
                            "compte epargne", "\u0627\u062F\u062E\u0627\u0631"),
                    keywords("deposit", "placement"),
                    keywords("bill", "shopping", "fuel"),
                    keywords("fund")
            ),
            keywordRule(
                    TransactionCategory.BANQUE,
                    keywords("transfer", "transfert", "virement", "western union", "moneygram", "flouci", "d17",
                            "\u062A\u062D\u0648\u064A\u0644"),
                    keywords("send money", "cash out"),
                    keywords("salary", "salaire", "assurance voiture", "assurance auto", "bill"),
                    keywords("transfer", "virement")
            ),
            keywordRule(
                    TransactionCategory.BANQUE,
                    keywords("bank fee", "frais bancaire", "commission bancaire", "tenue de compte", "agios",
                            "atm fee", "withdrawal fee", "chargeback", "carte bancaire", "frais compte"),
                    keywords("fee", "penalty", "commission"),
                    keywords("restaurant", "fuel", "assurance voiture", "salaire"),
                    keywords("fee")
            )
    );

    public CategorizationResult categorize(String merchantName, String description) {
        String normalizedMerchant = TransactionTextNormalizer.normalize(merchantName);
        String normalizedDescription = TransactionTextNormalizer.normalize(description);
        String normalizedText = TransactionTextNormalizer.normalize(merchantName, description);

        if (normalizedText.isBlank()) {
            return buildResult(TransactionCategory.AUTRES, 0.0d, "rule:no_text", normalizedText);
        }

        return findExactMerchantMatch(normalizedMerchant, normalizedText)
                .or(() -> findFuzzyMerchantMatch(normalizedMerchant))
                .or(() -> findPriorityRuleMatch(normalizedMerchant, normalizedDescription, normalizedText))
                .or(() -> findContextMatch(normalizedMerchant, normalizedDescription, normalizedText))
                .or(() -> findKeywordMatch(normalizedMerchant, normalizedDescription, normalizedText))
                .map(candidate -> {
                    log.debug(
                            "Rule categorization matched category={} confidence={} reason={} merchant='{}' description='{}'",
                            candidate.category(),
                            candidate.confidence(),
                            candidate.reason(),
                            merchantName,
                            description
                    );
                    return buildResult(candidate.category(), candidate.confidence(), candidate.reason(), normalizedText);
                })
                .orElseGet(() -> {
                    log.debug("No rule matched for merchant='{}' description='{}'", merchantName, description);
                    return buildResult(TransactionCategory.AUTRES, 0.0d, "rule:no_match", normalizedText);
                });
    }

    public TransactionCategory categorizeTransaction(String merchantName, String description) {
        return categorize(merchantName, description).getCategory();
    }

    public TransactionCategory categorizeByMerchant(String merchantName) {
        return categorizeTransaction(merchantName, "");
    }

    public TransactionCategory categorizeByDescription(String description) {
        return categorizeTransaction("", description);
    }

    private java.util.Optional<MatchCandidate> findExactMerchantMatch(String normalizedMerchant, String normalizedText) {
        if (normalizedMerchant.isBlank() && normalizedText.isBlank()) {
            return java.util.Optional.empty();
        }

        MatchCandidate bestMatch = null;
        for (MerchantRule rule : MERCHANT_RULES) {
            for (String alias : rule.aliases()) {
                MatchStrength strength = merchantMatchStrength(normalizedMerchant, alias);
                if (strength == MatchStrength.NONE) {
                    if (!matchesMerchantAlias(normalizedMerchant, normalizedText, Collections.singleton(alias))) {
                        continue;
                    }

                    MatchCandidate textCandidate = new MatchCandidate(
                            rule.category(),
                            rule.nearExactConfidence(),
                            95.0d,
                            "merchant:text:" + alias
                    );

                    if (isBetter(textCandidate, bestMatch)) {
                        bestMatch = textCandidate;
                    }
                    continue;
                }

                double confidence = strength == MatchStrength.EXACT
                        ? rule.exactConfidence()
                        : rule.nearExactConfidence();
                MatchCandidate candidate = new MatchCandidate(
                        rule.category(),
                        confidence,
                        100.0d,
                        "merchant:" + strength.name().toLowerCase(Locale.ROOT) + ":" + alias
                );

                if (isBetter(candidate, bestMatch)) {
                    bestMatch = candidate;
                }
            }
        }

        return java.util.Optional.ofNullable(bestMatch);
    }

    private java.util.Optional<MatchCandidate> findPriorityRuleMatch(
            String normalizedMerchant,
            String normalizedDescription,
            String normalizedText
    ) {
        MatchCandidate bestMatch = null;

        for (PriorityRule rule : PRIORITY_RULES) {
            if (containsAny(normalizedText, rule.negativeKeywords())) {
                continue;
            }

            boolean aliasMatch = matchesMerchantAlias(normalizedMerchant, normalizedText, rule.merchantAliases());
            boolean anyMatch = containsAny(normalizedText, rule.anyKeywords());
            boolean groupedMatch = matchesAllKeywordGroups(normalizedText, rule.allKeywordGroups());

            if (!rule.allKeywordGroups().isEmpty() && !groupedMatch) {
                continue;
            }

            if (!aliasMatch && !anyMatch && !groupedMatch) {
                continue;
            }

            double signal = 0.0d;
            if (aliasMatch) {
                signal += 3.0d;
            }
            if (groupedMatch) {
                signal += 2.0d + rule.allKeywordGroups().size();
            }
            if (anyMatch) {
                signal += 1.0d + countMatches(normalizedDescription, rule.anyKeywords()) * 0.25d;
            }

            MatchCandidate candidate = new MatchCandidate(
                    rule.category(),
                    rule.confidence(),
                    signal,
                    rule.reason()
            );

            if (isBetter(candidate, bestMatch)) {
                bestMatch = candidate;
            }
        }

        return java.util.Optional.ofNullable(bestMatch);
    }

    private java.util.Optional<MatchCandidate> findFuzzyMerchantMatch(String normalizedMerchant) {
        if (normalizedMerchant.isBlank()) {
            return java.util.Optional.empty();
        }

        String merchantKey = compact(normalizedMerchant);
        if (merchantKey.length() < 4) {
            return java.util.Optional.empty();
        }

        FuzzyMatch bestMatch = null;
        double runnerUp = 0.0d;

        for (MerchantRule rule : MERCHANT_RULES) {
            for (String alias : rule.aliases()) {
                String aliasKey = compact(alias);
                if (aliasKey.length() < 4) {
                    continue;
                }

                double similarity = similarity(merchantKey, aliasKey);
                if (similarity > runnerUp) {
                    if (bestMatch == null || similarity > bestMatch.similarity()) {
                        runnerUp = bestMatch == null ? runnerUp : bestMatch.similarity();
                        bestMatch = new FuzzyMatch(rule, alias, similarity);
                    } else {
                        runnerUp = similarity;
                    }
                }
            }
        }

        if (bestMatch == null) {
            return java.util.Optional.empty();
        }

        if (bestMatch.similarity() < FUZZY_MERCHANT_THRESHOLD || bestMatch.similarity() - runnerUp < FUZZY_MERCHANT_MARGIN) {
            return java.util.Optional.empty();
        }

        double confidence = clamp(
                0.90d + ((bestMatch.similarity() - FUZZY_MERCHANT_THRESHOLD) / (1.0d - FUZZY_MERCHANT_THRESHOLD)) * 0.06d,
                0.90d,
                bestMatch.rule().maxFuzzyConfidence()
        );

        return java.util.Optional.of(new MatchCandidate(
                bestMatch.rule().category(),
                confidence,
                bestMatch.similarity(),
                "merchant:fuzzy:" + bestMatch.alias()
        ));
    }

    private java.util.Optional<MatchCandidate> findContextMatch(
            String normalizedMerchant,
            String normalizedDescription,
            String normalizedText
    ) {
        if (normalizedText.isBlank()) {
            return java.util.Optional.empty();
        }

        MatchCandidate bestMatch = null;

        for (ContextRule rule : CONTEXT_RULES) {
            if (!merchantMatchesContextRule(normalizedMerchant, rule)) {
                continue;
            }
            if (containsAnyKeyword(normalizedText, rule.negativeKeywords())) {
                continue;
            }

            int descriptionHits = countMatches(normalizedDescription, rule.contextKeywords());
            int textHits = countMatches(normalizedText, rule.contextKeywords());
            int evidenceHits = Math.max(descriptionHits, textHits);
            if (evidenceHits == 0) {
                continue;
            }

            double confidence = clamp(
                    rule.baseConfidence() + Math.min(0.03d, (evidenceHits - 1) * 0.015d),
                    rule.baseConfidence(),
                    0.97d
            );

            MatchCandidate candidate = new MatchCandidate(
                    rule.category(),
                    confidence,
                    evidenceHits,
                    "context:" + rule.category()
            );

            if (isBetter(candidate, bestMatch)) {
                bestMatch = candidate;
            }
        }

        return java.util.Optional.ofNullable(bestMatch);
    }

    private java.util.Optional<MatchCandidate> findKeywordMatch(
            String normalizedMerchant,
            String normalizedDescription,
            String normalizedText
    ) {
        MatchCandidate bestMatch = null;

        for (KeywordRuleSet ruleSet : KEYWORD_RULES) {
            KeywordScore score = scoreKeywords(ruleSet, normalizedMerchant, normalizedDescription, normalizedText);
            if (score.confidence() <= 0.0d) {
                continue;
            }

            MatchCandidate candidate = new MatchCandidate(
                    ruleSet.category(),
                    score.confidence(),
                    score.rawScore(),
                    "keyword:" + ruleSet.category()
            );

            if (isBetter(candidate, bestMatch)) {
                bestMatch = candidate;
            }
        }

        return java.util.Optional.ofNullable(bestMatch);
    }

    private KeywordScore scoreKeywords(
            KeywordRuleSet ruleSet,
            String normalizedMerchant,
            String normalizedDescription,
            String normalizedText
    ) {
        double score = 0.0d;
        int strongMerchantHits = 0;
        int positiveMatches = 0;
        List<String> matchedWeakKeywords = new ArrayList<>();

        for (String keyword : ruleSet.strongKeywords()) {
            boolean merchantMatch = containsKeyword(normalizedMerchant, keyword);
            boolean descriptionMatch = containsKeyword(normalizedDescription, keyword);
            if (!merchantMatch && !descriptionMatch) {
                continue;
            }

            score += merchantMatch ? 3.20d : 0.0d;
            score += descriptionMatch ? 1.80d : 0.0d;
            if (merchantMatch) {
                strongMerchantHits++;
            }
            if (keyword.contains(" ") && containsKeyword(normalizedText, keyword)) {
                score += 0.25d;
            }
            if (normalizedMerchant.equals(keyword)) {
                score += 4.00d;
            } else if (normalizedDescription.equals(keyword) || normalizedText.equals(keyword)) {
                score += 5.50d;
            }
            positiveMatches++;
        }

        for (String keyword : ruleSet.weakKeywords()) {
            boolean merchantMatch = containsKeyword(normalizedMerchant, keyword);
            boolean descriptionMatch = containsKeyword(normalizedDescription, keyword);
            if (!merchantMatch && !descriptionMatch) {
                continue;
            }

            score += merchantMatch ? 1.80d : 0.0d;
            score += descriptionMatch ? 0.90d : 0.0d;
            if (keyword.contains(" ") && containsKeyword(normalizedText, keyword)) {
                score += 0.15d;
            }
            if (normalizedMerchant.equals(keyword)) {
                score += 1.50d;
            } else if (normalizedDescription.equals(keyword) || normalizedText.equals(keyword)) {
                score += 2.25d;
            }
            matchedWeakKeywords.add(keyword);
            positiveMatches++;
        }

        int negativeHits = countMatches(normalizedText, ruleSet.negativeKeywords());
        score -= negativeHits * 2.40d;

        boolean weakOnlyStandalone = positiveMatches == 1
                && strongMerchantHits == 0
                && matchedWeakKeywords.size() == 1
                && ruleSet.standaloneWeakKeywords().contains(matchedWeakKeywords.get(0));

        if (weakOnlyStandalone) {
            score = Math.min(score, 1.25d);
        }

        if (score < KEYWORD_MIN_SCORE) {
            return new KeywordScore(0.0d, score);
        }

        double confidence = clamp(0.52d + (score * 0.055d), 0.0d, KEYWORD_MAX_CONFIDENCE);
        if (strongMerchantHits > 0) {
            confidence += 0.03d;
        }
        if (positiveMatches >= 2) {
            confidence += 0.02d;
        }
        confidence -= negativeHits * 0.04d;

        return new KeywordScore(clamp(confidence, 0.0d, KEYWORD_MAX_CONFIDENCE), score);
    }

    private boolean merchantMatchesContextRule(String normalizedMerchant, ContextRule rule) {
        if (normalizedMerchant.isBlank()) {
            return false;
        }

        if (matchesMerchantAlias(normalizedMerchant, normalizedMerchant, rule.merchantHints())) {
            return true;
        }

        String merchantKey = compact(normalizedMerchant);
        for (String hint : rule.merchantHints()) {
            String hintKey = compact(hint);
            if (merchantKey.length() >= 4
                    && hintKey.length() >= 4
                    && similarity(merchantKey, hintKey) >= 0.89d) {
                return true;
            }
        }

        return false;
    }

    private MatchStrength merchantMatchStrength(String normalizedMerchant, String alias) {
        if (normalizedMerchant.isBlank() || alias.isBlank()) {
            return MatchStrength.NONE;
        }

        String merchantKey = compact(normalizedMerchant);
        String aliasKey = compact(alias);
        if (merchantKey.equals(aliasKey)) {
            return MatchStrength.EXACT;
        }

        if (containsWholeKeyword(normalizedMerchant, alias)) {
            int merchantTokens = tokenCount(normalizedMerchant);
            int aliasTokens = tokenCount(alias);
            if (merchantTokens - aliasTokens <= 2) {
                return MatchStrength.NEAR_EXACT;
            }
        }

        return MatchStrength.NONE;
    }

    private boolean containsAnyKeyword(String normalizedText, Collection<String> keywords) {
        return containsAny(normalizedText, keywords);
    }

    private boolean containsAny(String normalizedText, Collection<String> keywords) {
        for (String keyword : keywords) {
            if (containsKeyword(normalizedText, keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAll(String normalizedText, Collection<String> keywords) {
        if (keywords.isEmpty()) {
            return true;
        }
        for (String keyword : keywords) {
            if (!containsKeyword(normalizedText, keyword)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesMerchantAlias(
            String normalizedMerchant,
            String normalizedText,
            Collection<String> aliases
    ) {
        for (String alias : aliases) {
            MatchStrength merchantStrength = merchantMatchStrength(normalizedMerchant, alias);
            if (merchantStrength != MatchStrength.NONE) {
                return true;
            }
            if (containsKeyword(normalizedText, alias)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAllKeywordGroups(String normalizedText, Collection<Set<String>> keywordGroups) {
        if (keywordGroups.isEmpty()) {
            return false;
        }
        for (Set<String> group : keywordGroups) {
            if (!containsAny(normalizedText, group) && !containsAll(normalizedText, group)) {
                return false;
            }
        }
        return true;
    }

    private int countMatches(String normalizedText, Collection<String> keywords) {
        int matches = 0;
        for (String keyword : keywords) {
            if (containsKeyword(normalizedText, keyword)) {
                matches++;
            }
        }
        return matches;
    }

    private boolean containsKeyword(String normalizedText, String keyword) {
        return containsWholeKeyword(normalizedText, keyword);
    }

    private boolean containsWholeKeyword(String normalizedText, String keyword) {
        if (normalizedText.isBlank() || keyword.isBlank()) {
            return false;
        }
        String paddedText = " " + normalizedText + " ";
        String paddedKeyword = " " + keyword + " ";
        return paddedText.contains(paddedKeyword);
    }

    private boolean isBetter(MatchCandidate candidate, MatchCandidate currentBest) {
        if (candidate == null) {
            return false;
        }
        if (currentBest == null) {
            return true;
        }
        if (candidate.confidence() != currentBest.confidence()) {
            return candidate.confidence() > currentBest.confidence();
        }
        return candidate.signal() > currentBest.signal();
    }

    private double similarity(String left, String right) {
        return Math.max(jaroWinkler(left, right), levenshteinSimilarity(left, right));
    }

    private double levenshteinSimilarity(String left, String right) {
        if (left.equals(right)) {
            return 1.0d;
        }
        int maxLength = Math.max(left.length(), right.length());
        if (maxLength == 0) {
            return 1.0d;
        }
        return 1.0d - ((double) levenshteinDistance(left, right) / maxLength);
    }

    private int levenshteinDistance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];

        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost
                );
            }

            int[] temp = previous;
            previous = current;
            current = temp;
        }

        return previous[right.length()];
    }

    private double jaroWinkler(String left, String right) {
        if (left.equals(right)) {
            return 1.0d;
        }
        if (left.isBlank() || right.isBlank()) {
            return 0.0d;
        }

        int matchDistance = Math.max(0, Math.max(left.length(), right.length()) / 2 - 1);
        boolean[] leftMatches = new boolean[left.length()];
        boolean[] rightMatches = new boolean[right.length()];

        int matches = 0;
        for (int i = 0; i < left.length(); i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, right.length());
            for (int j = start; j < end; j++) {
                if (rightMatches[j] || left.charAt(i) != right.charAt(j)) {
                    continue;
                }
                leftMatches[i] = true;
                rightMatches[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) {
            return 0.0d;
        }

        double transpositions = 0.0d;
        int rightIndex = 0;
        for (int i = 0; i < left.length(); i++) {
            if (!leftMatches[i]) {
                continue;
            }
            while (!rightMatches[rightIndex]) {
                rightIndex++;
            }
            if (left.charAt(i) != right.charAt(rightIndex)) {
                transpositions++;
            }
            rightIndex++;
        }

        double m = matches;
        double jaro = ((m / left.length()) + (m / right.length()) + ((m - (transpositions / 2.0d)) / m)) / 3.0d;

        int prefix = 0;
        int maxPrefix = Math.min(4, Math.min(left.length(), right.length()));
        while (prefix < maxPrefix && left.charAt(prefix) == right.charAt(prefix)) {
            prefix++;
        }

        return jaro + (prefix * 0.1d * (1.0d - jaro));
    }

    private int tokenCount(String text) {
        return text.isBlank() ? 0 : text.split(" ").length;
    }

    private String compact(String text) {
        return COMPACT_PATTERN.matcher(text).replaceAll("");
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private CategorizationResult buildResult(
            TransactionCategory category,
            double confidence,
            String reason,
            String normalizedText
    ) {
        return CategorizationResult.builder()
                .category(category)
                .confidence(confidence)
                .source(CategorizationSources.RULE_ENGINE)
                .reason(reason)
                .normalizedText(normalizedText)
                .build();
    }

    private static MerchantRule merchantRule(TransactionCategory category, String... aliases) {
        return new MerchantRule(
                category,
                normalizedKeywords(aliases),
                EXACT_MERCHANT_CONFIDENCE,
                NEAR_EXACT_MERCHANT_CONFIDENCE,
                0.96d
        );
    }

    private static ContextRule contextRule(
            TransactionCategory category,
            double baseConfidence,
            Set<String> merchantHints,
            Set<String> contextKeywords,
            Set<String> negativeKeywords
    ) {
        return new ContextRule(category, merchantHints, contextKeywords, negativeKeywords, baseConfidence);
    }

    private static PriorityRule priorityRule(
            TransactionCategory category,
            double confidence,
            String reason,
            Set<String> merchantAliases,
            Set<String> anyKeywords,
            List<Set<String>> allKeywordGroups,
            Set<String> negativeKeywords
    ) {
        return new PriorityRule(
                category,
                confidence,
                reason,
                merchantAliases,
                anyKeywords,
                List.copyOf(allKeywordGroups),
                negativeKeywords
        );
    }

    private static KeywordRuleSet keywordRule(
            TransactionCategory category,
            Set<String> strongKeywords,
            Set<String> weakKeywords,
            Set<String> negativeKeywords,
            Set<String> standaloneWeakKeywords
    ) {
        return new KeywordRuleSet(category, strongKeywords, weakKeywords, negativeKeywords, standaloneWeakKeywords);
    }

    private static Set<String> keywords(String... values) {
        return normalizedKeywords(values);
    }

    private static Set<String> normalizedKeywords(String... values) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String keyword = TransactionTextNormalizer.normalize(value);
            if (!keyword.isBlank()) {
                normalized.add(keyword);
            }
        }
        return Collections.unmodifiableSet(normalized);
    }

    private record MerchantRule(
            TransactionCategory category,
            Set<String> aliases,
            double exactConfidence,
            double nearExactConfidence,
            double maxFuzzyConfidence
    ) {
    }

    private record ContextRule(
            TransactionCategory category,
            Set<String> merchantHints,
            Set<String> contextKeywords,
            Set<String> negativeKeywords,
            double baseConfidence
    ) {
    }

    private record PriorityRule(
            TransactionCategory category,
            double confidence,
            String reason,
            Set<String> merchantAliases,
            Set<String> anyKeywords,
            List<Set<String>> allKeywordGroups,
            Set<String> negativeKeywords
    ) {
    }

    private record KeywordRuleSet(
            TransactionCategory category,
            Set<String> strongKeywords,
            Set<String> weakKeywords,
            Set<String> negativeKeywords,
            Set<String> standaloneWeakKeywords
    ) {
    }

    private record MatchCandidate(TransactionCategory category, double confidence, double signal, String reason) {
    }

    private record FuzzyMatch(MerchantRule rule, String alias, double similarity) {
    }

    private record KeywordScore(double confidence, double rawScore) {
    }

    private enum MatchStrength {
        NONE,
        NEAR_EXACT,
        EXACT
    }
}


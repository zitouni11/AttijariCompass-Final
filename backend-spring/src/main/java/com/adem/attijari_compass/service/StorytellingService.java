package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.storytelling.MonthlyStoryResponse;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.repository.TransactionRepository;
import com.adem.attijari_compass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StorytellingService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public MonthlyStoryResponse getMonthlyStory(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LocalDate debut = LocalDate.now().withDayOfMonth(1);
        LocalDate fin = LocalDate.now();

        Double totalRevenu = transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                user.getId(), TransactionType.REVENU, debut, fin);
        Double totalDepenses = transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                user.getId(), TransactionType.DEPENSE, debut, fin);

        totalRevenu = totalRevenu != null ? totalRevenu : 0.0;
        totalDepenses = totalDepenses != null ? totalDepenses : 0.0;
        double epargne = totalRevenu - totalDepenses;

        Map<String, Double> depensesParCat = new HashMap<>();
        for (TransactionCategory category : TransactionCategory.values()) {
            Double montant = transactionRepository.sumAmountByUserIdAndCategoryAndDateBetween(
                    user.getId(), category, debut, fin);
            if (montant != null && montant > 0) {
                depensesParCat.put(category.name(), montant);
            }
        }

        String categoriesPrincipales = depensesParCat.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .map(entry -> TransactionCategory.fromValue(entry.getKey()).label()
                        + " (" + String.format(Locale.ROOT, "%.0f", entry.getValue()) + " DT)")
                .collect(Collectors.joining(", "));

        return MonthlyStoryResponse.builder()
                .mois(LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy")))
                .resume(genererResume(user.getEmail(), totalRevenu, totalDepenses, epargne, depensesParCat))
                .categoriesPrincipales(categoriesPrincipales)
                .totalDepenses(totalDepenses)
                .totalRevenus(totalRevenu)
                .epargneRealisee(epargne)
                .alertes(genererAlertes(totalRevenu, totalDepenses, epargne, depensesParCat))
                .missions(genererMissions(totalRevenu, totalDepenses, depensesParCat))
                .build();
    }

    private String genererResume(
            String email,
            double revenu,
            double depenses,
            double epargne,
            Map<String, Double> categories
    ) {
        String prenom = email.split("@")[0];
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Ce mois-ci, %s, vos finances racontent une belle histoire. ", prenom));

        if (revenu > 0) {
            sb.append(String.format(Locale.ROOT,
                    "Vous avez percu %.0f DT de revenus et depense %.0f DT. ",
                    revenu,
                    depenses));
        }

        if (!categories.isEmpty()) {
            String topCategorie = categories.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(TransactionCategory.fallback().name());
            sb.append(String.format("Votre argent a surtout finance le poste %s. ",
                    TransactionCategory.fromValue(topCategorie).lowerLabel()));
        }

        if (epargne > 0) {
            sb.append(String.format(Locale.ROOT,
                    "Felicitations, vous avez reussi a epargner %.0f DT ce mois-ci. ",
                    epargne));
            double tauxEpargne = revenu > 0 ? (epargne / revenu) * 100 : 0;
            sb.append(tauxEpargne >= 20
                    ? "Vous etes sur une trajectoire financiere solide."
                    : "Chaque dirham mis de cote vous rapproche de vos objectifs.");
        } else if (epargne < 0) {
            sb.append(String.format(Locale.ROOT,
                    "Attention, vous avez depense %.0f DT de plus que vos revenus. ",
                    Math.abs(epargne)));
            sb.append("Il est temps de revoir le budget du mois prochain.");
        } else {
            sb.append("Vous avez equilibre revenus et depenses ce mois-ci.");
        }

        return sb.toString();
    }

    private List<String> genererAlertes(
            double revenu,
            double depenses,
            double epargne,
            Map<String, Double> categories
    ) {
        List<String> alertes = new ArrayList<>();

        if (epargne < 0) {
            alertes.add(String.format(Locale.ROOT,
                    "Votre budget est deficitaire de %.0f DT ce mois. Reduisez les depenses non essentielles.",
                    Math.abs(epargne)));
        }

        double restauration = safeCategoryTotal(categories, TransactionCategory.RESTAURANT, TransactionCategory.CAFES, TransactionCategory.LIVRAISON);
        if (restauration > 0 && revenu > 0 && (restauration / revenu) * 100 > 25) {
            alertes.add(String.format(Locale.ROOT,
                    "Les depenses cafes et livraison (%.0f DT) depassent 25%% de vos revenus.",
                    restauration));
        }

        Double shopping = categories.get(TransactionCategory.SHOPPING.name());
        if (shopping != null && revenu > 0 && (shopping / revenu) * 100 > 30) {
            alertes.add(String.format(Locale.ROOT,
                    "Vos depenses shopping (%.0f DT) sont elevees ce mois. Avant d'acheter, reposez-vous la question du besoin reel.",
                    shopping));
        }

        if (revenu > 0 && ((revenu - depenses) / revenu) * 100 < 10) {
            alertes.add("Votre taux d'epargne est faible. Essayez de mieux separer besoins essentiels et depenses plaisir.");
        }

        return alertes;
    }

    private List<String> genererMissions(double revenu, double depenses, Map<String, Double> categories) {
        List<String> missions = new ArrayList<>();
        missions.add("Mission Epargne : mettez de cote 10% de vos revenus ce mois-ci.");

        double restauration = safeCategoryTotal(categories, TransactionCategory.RESTAURANT, TransactionCategory.CAFES, TransactionCategory.LIVRAISON);
        if (restauration > 0 && revenu > 0 && (restauration / revenu) * 100 > 15) {
            missions.add("Mission Equilibre : remplacez 3 sorties cafes ou livraisons par des repas prepares a la maison cette semaine.");
        }

        if (revenu > 0 && ((revenu - depenses) / revenu) * 100 < 20) {
            missions.add("Mission Budget : reduisez vos depenses de 10% le mois prochain.");
        }

        missions.add("Mission Suivi : enregistrez toutes vos depenses cette semaine.");
        return missions;
    }

    private double safeCategoryTotal(Map<String, Double> categories, TransactionCategory... categoryCandidates) {
        double total = 0.0;
        if (categories == null || categoryCandidates == null) {
            return total;
        }

        for (TransactionCategory category : categoryCandidates) {
            if (category == null) {
                continue;
            }
            total += categories.getOrDefault(category.name(), 0.0);
        }
        return total;
    }
}

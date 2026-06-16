package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.recommendation.RecommendationResponse;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.recommendation.enums.RecommendationSourceType;
import com.adem.attijari_compass.repository.TransactionRepository;
import com.adem.attijari_compass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public List<RecommendationResponse> getRecommendations(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LocalDate debut = LocalDate.now().withDayOfMonth(1);
        LocalDate fin = LocalDate.now();

        Double totalRevenu = transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                user.getId(), TransactionType.REVENU, debut, fin);
        totalRevenu = totalRevenu != null ? totalRevenu : 0.0;

        List<RecommendationResponse> recommendations = new ArrayList<>();

        double depensesRestauration = safeSum(
                transactionRepository.sumAmountByUserIdAndCategoryAndDateBetween(user.getId(), TransactionCategory.RESTAURANT, debut, fin),
                transactionRepository.sumAmountByUserIdAndCategoryAndDateBetween(user.getId(), TransactionCategory.CAFES, debut, fin),
                transactionRepository.sumAmountByUserIdAndCategoryAndDateBetween(user.getId(), TransactionCategory.LIVRAISON, debut, fin)
        );
        if (depensesRestauration > 0 && totalRevenu > 0 && (depensesRestauration / totalRevenu) * 100 > 25) {
            recommendations.add(RecommendationResponse.builder()
                    .categorie(TransactionCategory.RESTAURANT.name())
                    .message(String.format(Locale.ROOT,
                            "Vos depenses restaurant, cafes et livraison representent %.1f%% de vos revenus.",
                            (depensesRestauration / totalRevenu) * 100))
                    .suggestion("Essayez de cuisiner davantage a la maison et de regrouper les petites commandes de la semaine.")
                    .gainEstimeEnMois(depensesRestauration * 0.4)
                    .priorite("HAUTE")
                    .sourceType(RecommendationSourceType.EXPENSE.name())
                    .build());
        }

        Double depensesDivertissement = transactionRepository.sumAmountByUserIdAndCategoryAndDateBetween(
                user.getId(), TransactionCategory.DIVERTISSEMENT, debut, fin);
        if (depensesDivertissement != null && totalRevenu > 0
                && (depensesDivertissement / totalRevenu) * 100 > 20) {
            recommendations.add(RecommendationResponse.builder()
                    .categorie(TransactionCategory.DIVERTISSEMENT.name())
                    .message(String.format(Locale.ROOT,
                            "Vos depenses divertissement representent %.1f%% de vos revenus.",
                            (depensesDivertissement / totalRevenu) * 100))
                    .suggestion("Cherchez des activites gratuites ou a cout reduit ce mois-ci.")
                    .gainEstimeEnMois(depensesDivertissement * 0.3)
                    .priorite("MOYENNE")
                    .sourceType(RecommendationSourceType.EXPENSE.name())
                    .build());
        }

        Double depensesShopping = transactionRepository.sumAmountByUserIdAndCategoryAndDateBetween(
                user.getId(), TransactionCategory.SHOPPING, debut, fin);
        if (depensesShopping != null && totalRevenu > 0
                && (depensesShopping / totalRevenu) * 100 > 30) {
            recommendations.add(RecommendationResponse.builder()
                    .categorie(TransactionCategory.SHOPPING.name())
                    .message(String.format(Locale.ROOT,
                            "Vos depenses shopping representent %.1f%% de vos revenus.",
                            (depensesShopping / totalRevenu) * 100))
                    .suggestion("Adoptez la regle des 30 jours avant tout achat non essentiel.")
                    .gainEstimeEnMois(depensesShopping * 0.5)
                    .priorite("HAUTE")
                    .sourceType(RecommendationSourceType.EXPENSE.name())
                    .build());
        }

        Double totalDepenses = transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                user.getId(), TransactionType.DEPENSE, debut, fin);
        totalDepenses = totalDepenses != null ? totalDepenses : 0.0;
        if (totalRevenu > 0 && ((totalRevenu - totalDepenses) / totalRevenu) * 100 < 10) {
            recommendations.add(RecommendationResponse.builder()
                    .categorie(TransactionCategory.AUTRES.name())
                    .message("Votre taux d'epargne est inferieur a 10% ce mois-ci.")
                    .suggestion("Essayez d'appliquer une regle simple de repartition du revenu pour renforcer votre marge d'epargne.")
                    .gainEstimeEnMois(totalRevenu * 0.10)
                    .priorite("HAUTE")
                    .sourceType(RecommendationSourceType.EXPENSE.name())
                    .build());
        }

        if (recommendations.isEmpty()) {
            recommendations.add(RecommendationResponse.builder()
                    .categorie("GENERAL")
                    .message("Felicitations, vos finances restent bien gerees ce mois-ci.")
                    .suggestion("Continuez ainsi et consolidez progressivement votre epargne.")
                    .gainEstimeEnMois(0.0)
                    .priorite("BASSE")
                    .sourceType(RecommendationSourceType.EXPENSE.name())
                    .build());
        }

        return recommendations;
    }

    private double safeSum(Double... values) {
        double total = 0.0;
        if (values == null) {
            return total;
        }
        for (Double value : values) {
            total += value != null ? value : 0.0;
        }
        return total;
    }
}

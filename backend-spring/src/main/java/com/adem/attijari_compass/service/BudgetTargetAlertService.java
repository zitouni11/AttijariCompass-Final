package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.budget.BudgetAlertResponse;
import com.adem.attijari_compass.dto.budget.BudgetTargetResponse;
import com.adem.attijari_compass.entity.BudgetAlertSeverity;
import com.adem.attijari_compass.entity.BudgetAlertType;
import com.adem.attijari_compass.entity.BudgetMonitoringStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BudgetTargetAlertService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal QUASI_THRESHOLD = new BigDecimal("85");
    private static final BigDecimal STRONG_CONTROL_THRESHOLD = new BigDecimal("50");
    private static final BigDecimal LOW_REMAINING_RATIO = new BigDecimal("0.15");

    private final BudgetTargetService budgetTargetService;

    public List<BudgetAlertResponse> getAlertsForCurrentUser(String email) {
        List<BudgetTargetResponse> budgets = budgetTargetService.getActiveBudgetTargetsForCurrentUser(email);
        if (budgets.isEmpty()) {
            log.info("Budget alerts requested but no active budgets found for user {}", email);
            return List.of();
        }

        LocalDateTime generatedAt = LocalDateTime.now();
        List<BudgetAlertResponse> alerts = new ArrayList<>();

        for (BudgetTargetResponse budget : budgets) {
            BudgetAlertResponse alert = buildPrimaryAlert(budget, generatedAt);
            if (alert != null) {
                alerts.add(alert);
            }
        }

        BudgetAlertResponse priorityAlert = buildPriorityAlert(budgets, generatedAt);
        if (priorityAlert != null) {
            alerts.add(priorityAlert);
        }

        if (alerts.isEmpty()) {
            BudgetAlertResponse globalMasteryAlert = buildGlobalMasteryAlert(budgets, generatedAt);
            if (globalMasteryAlert != null) {
                alerts.add(globalMasteryAlert);
            } else {
                BudgetAlertResponse positiveAlert = buildUnderControlAlert(selectBestControlledBudget(budgets), generatedAt);
                if (positiveAlert != null) {
                    alerts.add(positiveAlert);
                }
            }
        }

        List<BudgetAlertResponse> sortedAlerts = alerts.stream()
                .filter(Objects::nonNull)
                .sorted(
                        Comparator.comparing((BudgetAlertResponse alert) -> alert.getSeverity().getWeight()).reversed()
                                .thenComparing(BudgetAlertResponse::getPriorityRank, Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(BudgetAlertResponse::getGeneratedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .toList();

        log.info("Budget alerts generated: userEmail={}, budgetCount={}, alertCount={}",
                email,
                budgets.size(),
                sortedAlerts.size());

        return sortedAlerts;
    }

    private BudgetAlertResponse buildPrimaryAlert(BudgetTargetResponse budget, LocalDateTime generatedAt) {
        if (budget == null || budget.getUsagePercent() == null) {
            return null;
        }

        if (isExceeded(budget)) {
            return buildExceededAlert(budget, generatedAt);
        }
        if (isQuasiReached(budget)) {
            return buildQuasiReachedAlert(budget, generatedAt);
        }
        if (hasLowRemainingAmount(budget)) {
            return buildLowRemainingAlert(budget, generatedAt);
        }
        return null;
    }

    private BudgetAlertResponse buildPriorityAlert(List<BudgetTargetResponse> budgets, LocalDateTime generatedAt) {
        if (budgets == null || budgets.size() < 2) {
            return null;
        }

        BudgetTargetResponse candidate = budgets.stream()
                .filter(this::isRelevantPriorityCandidate)
                .max(Comparator.comparing(this::riskScore))
                .orElse(null);

        if (candidate == null) {
            return null;
        }

        BudgetAlertSeverity severity = isExceeded(candidate) ? BudgetAlertSeverity.CRITICAL : BudgetAlertSeverity.WARNING;
        String message = String.format(
                Locale.ROOT,
                "La categorie %s concentre aujourd'hui le principal risque budgetaire de votre mois.",
                safeCategoryLabel(candidate)
        );

        return buildAlert(
                BudgetAlertType.BUDGET_CRITIQUE_PRIORITAIRE,
                severity,
                candidate,
                "Budget prioritaire a surveiller",
                message,
                priorityBase(severity) + 500 + riskScore(candidate).intValue(),
                generatedAt
        );
    }

    private BudgetAlertResponse buildGlobalMasteryAlert(List<BudgetTargetResponse> budgets, LocalDateTime generatedAt) {
        boolean allUnderControl = budgets.stream().allMatch(this::isUnderControl);
        if (!allUnderControl) {
            return null;
        }

        long budgetsCount = budgets.stream().filter(Objects::nonNull).count();
        return BudgetAlertResponse.builder()
                .alertType(BudgetAlertType.BUDGET_MAITRISE_GLOBALE)
                .alertTypeLabel(BudgetAlertType.BUDGET_MAITRISE_GLOBALE.getLabel())
                .severity(BudgetAlertSeverity.INFO)
                .severityLabel(BudgetAlertSeverity.INFO.getLabel())
                .title("Bonne maitrise budgetaire")
                .message(String.format(
                        Locale.ROOT,
                        "Vos %d budgets actifs restent sous controle ce mois-ci. La trajectoire est saine.",
                        budgetsCount
                ))
                .priorityRank(priorityBase(BudgetAlertSeverity.INFO) + 150)
                .generatedAt(generatedAt)
                .build();
    }

    private BudgetAlertResponse buildUnderControlAlert(BudgetTargetResponse budget, LocalDateTime generatedAt) {
        if (budget == null || budget.getUsagePercent() == null || budget.getUsagePercent().compareTo(STRONG_CONTROL_THRESHOLD) >= 0) {
            return null;
        }

        String message = String.format(
                Locale.ROOT,
                "Votre enveloppe %s reste bien maitrisee ce mois-ci avec une marge confortable.",
                safeCategoryLabel(budget).toLowerCase(Locale.ROOT)
        );

        return buildAlert(
                BudgetAlertType.BUDGET_SOUS_CONTROLE,
                BudgetAlertSeverity.INFO,
                budget,
                "Budget sous controle",
                message,
                priorityBase(BudgetAlertSeverity.INFO) + controlScore(budget).intValue(),
                generatedAt
        );
    }

    private BudgetAlertResponse buildExceededAlert(BudgetTargetResponse budget, LocalDateTime generatedAt) {
        BigDecimal exceededAmount = positiveOrZero(safeSpent(budget).subtract(safeTarget(budget)));
        String message = String.format(
                Locale.ROOT,
                "Vos depenses ont depasse le cadre prevu pour cette categorie. Depassement estime : %s DT.",
                formatMoney(exceededAmount)
        );

        return buildAlert(
                BudgetAlertType.BUDGET_DEPASSE,
                BudgetAlertSeverity.CRITICAL,
                budget,
                "Budget depasse",
                message,
                priorityBase(BudgetAlertSeverity.CRITICAL) + riskScore(budget).intValue(),
                generatedAt
        );
    }

    private BudgetAlertResponse buildQuasiReachedAlert(BudgetTargetResponse budget, LocalDateTime generatedAt) {
        String message = String.format(
                Locale.ROOT,
                "Votre cadre approche de sa limite mensuelle. Reste disponible : %s DT.",
                formatMoney(safeRemaining(budget))
        );

        return buildAlert(
                BudgetAlertType.BUDGET_QUASI_ATTEINT,
                BudgetAlertSeverity.WARNING,
                budget,
                "Budget presque atteint",
                message,
                priorityBase(BudgetAlertSeverity.WARNING) + riskScore(budget).intValue(),
                generatedAt
        );
    }

    private BudgetAlertResponse buildLowRemainingAlert(BudgetTargetResponse budget, LocalDateTime generatedAt) {
        String message = String.format(
                Locale.ROOT,
                "La marge restante devient faible sur cette categorie. Il ne reste plus que %s DT disponibles.",
                formatMoney(safeRemaining(budget))
        );

        return buildAlert(
                BudgetAlertType.BUDGET_RESTE_FAIBLE,
                BudgetAlertSeverity.WARNING,
                budget,
                "Reste mensuel faible",
                message,
                priorityBase(BudgetAlertSeverity.WARNING) + 500 + riskScore(budget).intValue(),
                generatedAt
        );
    }

    private BudgetAlertResponse buildAlert(BudgetAlertType alertType,
                                           BudgetAlertSeverity severity,
                                           BudgetTargetResponse budget,
                                           String title,
                                           String message,
                                           Integer priorityRank,
                                           LocalDateTime generatedAt) {
        return BudgetAlertResponse.builder()
                .alertType(alertType)
                .alertTypeLabel(alertType.getLabel())
                .severity(severity)
                .severityLabel(severity.getLabel())
                .budgetTargetId(budget != null ? budget.getId() : null)
                .category(budget != null ? budget.getCategory() : null)
                .categoryLabel(budget != null ? safeCategoryLabel(budget) : null)
                .title(title)
                .message(message)
                .usagePercent(budget != null ? budget.getUsagePercent() : null)
                .targetAmount(budget != null ? budget.getTargetAmount() : null)
                .spentThisMonth(budget != null ? budget.getSpentThisMonth() : null)
                .remainingAmount(budget != null ? budget.getRemainingAmount() : null)
                .priorityRank(priorityRank)
                .generatedAt(generatedAt)
                .build();
    }

    private boolean isExceeded(BudgetTargetResponse budget) {
        return budget.getMonitoringStatus() == BudgetMonitoringStatus.DEPASSE
                || budget.getUsagePercent().compareTo(HUNDRED) > 0;
    }

    private boolean isQuasiReached(BudgetTargetResponse budget) {
        return budget.getUsagePercent().compareTo(QUASI_THRESHOLD) >= 0
                && budget.getUsagePercent().compareTo(HUNDRED) <= 0;
    }

    private boolean hasLowRemainingAmount(BudgetTargetResponse budget) {
        BigDecimal targetAmount = safeTarget(budget);
        BigDecimal remainingAmount = budget.getRemainingAmount();
        if (targetAmount.signum() <= 0 || remainingAmount == null || remainingAmount.signum() <= 0) {
            return false;
        }
        BigDecimal threshold = targetAmount.multiply(LOW_REMAINING_RATIO).setScale(2, RoundingMode.HALF_UP);
        return remainingAmount.compareTo(threshold) <= 0;
    }

    private boolean isRelevantPriorityCandidate(BudgetTargetResponse budget) {
        return budget != null
                && budget.getUsagePercent() != null
                && (isExceeded(budget) || isQuasiReached(budget) || hasLowRemainingAmount(budget));
    }

    private boolean isUnderControl(BudgetTargetResponse budget) {
        return budget != null
                && budget.getMonitoringStatus() == BudgetMonitoringStatus.SOUS_CONTROLE
                && budget.getUsagePercent() != null
                && budget.getUsagePercent().compareTo(HUNDRED) < 0;
    }

    private BudgetTargetResponse selectBestControlledBudget(List<BudgetTargetResponse> budgets) {
        return budgets.stream()
                .filter(this::isUnderControl)
                .min(Comparator.comparing(BudgetTargetResponse::getUsagePercent))
                .orElse(null);
    }

    private BigDecimal riskScore(BudgetTargetResponse budget) {
        return budget.getUsagePercent() != null ? budget.getUsagePercent() : ZERO;
    }

    private BigDecimal controlScore(BudgetTargetResponse budget) {
        if (budget == null || budget.getUsagePercent() == null) {
            return ZERO;
        }
        return HUNDRED.subtract(budget.getUsagePercent());
    }

    private int priorityBase(BudgetAlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 3000;
            case WARNING -> 2000;
            case INFO -> 1000;
        };
    }

    private BigDecimal safeTarget(BudgetTargetResponse budget) {
        return budget != null && budget.getTargetAmount() != null ? budget.getTargetAmount() : ZERO;
    }

    private BigDecimal safeSpent(BudgetTargetResponse budget) {
        return budget != null && budget.getSpentThisMonth() != null ? budget.getSpentThisMonth() : ZERO;
    }

    private BigDecimal safeRemaining(BudgetTargetResponse budget) {
        return budget != null && budget.getRemainingAmount() != null ? budget.getRemainingAmount() : ZERO;
    }

    private BigDecimal positiveOrZero(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            return ZERO;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String safeCategoryLabel(BudgetTargetResponse budget) {
        if (budget == null) {
            return "Budget";
        }
        if (budget.getCategoryLabel() != null && !budget.getCategoryLabel().isBlank()) {
            return budget.getCategoryLabel();
        }
        return budget.getCategory() != null ? budget.getCategory().name() : "Budget";
    }

    private String formatMoney(BigDecimal amount) {
        BigDecimal normalized = amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : ZERO;
        return normalized.stripTrailingZeros().scale() > 0
                ? normalized.stripTrailingZeros().toPlainString()
                : normalized.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}

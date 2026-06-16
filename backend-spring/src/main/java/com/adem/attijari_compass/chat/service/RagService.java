package com.adem.attijari_compass.chat.service;

import com.adem.attijari_compass.dto.budget.BudgetAlertResponse;
import com.adem.attijari_compass.dto.budget.BudgetTargetResponse;
import com.adem.attijari_compass.dto.goal.GoalAiRefreshResponse;
import com.adem.attijari_compass.dto.goal.GoalResponse;
import com.adem.attijari_compass.dto.report.ReportSummaryResponse;
import com.adem.attijari_compass.entity.GoalStatus;
import com.adem.attijari_compass.entity.PaymentMethod;
import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionSource;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.entity.UserCard;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.recommendation.dto.RecommendationDto;
import com.adem.attijari_compass.recommendation.dto.RecommendationResponseDto;
import com.adem.attijari_compass.recommendation.dto.RecommendationSummaryDto;
import com.adem.attijari_compass.recommendation.service.RecommendationService;
import com.adem.attijari_compass.repository.FinancialGoalRepository;
import com.adem.attijari_compass.repository.TransactionRepository;
import com.adem.attijari_compass.repository.UserCardRepository;
import com.adem.attijari_compass.repository.UserRepository;
import com.adem.attijari_compass.service.BudgetTargetAlertService;
import com.adem.attijari_compass.service.BudgetTargetService;
import com.adem.attijari_compass.service.GoalService;
import com.adem.attijari_compass.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b");
    private static final int MAX_CONTEXT_LENGTH = 6500;
    private static final int MAX_TRANSACTION_EXAMPLES = 10;

    private static final Map<String, Integer> MONTH_TOKENS = Map.ofEntries(
            Map.entry("janvier", 1),
            Map.entry("jan", 1),
            Map.entry("january", 1),
            Map.entry("fevrier", 2),
            Map.entry("fev", 2),
            Map.entry("february", 2),
            Map.entry("mars", 3),
            Map.entry("march", 3),
            Map.entry("avril", 4),
            Map.entry("avr", 4),
            Map.entry("april", 4),
            Map.entry("mai", 5),
            Map.entry("may", 5),
            Map.entry("juin", 6),
            Map.entry("june", 6),
            Map.entry("juillet", 7),
            Map.entry("juil", 7),
            Map.entry("july", 7),
            Map.entry("aout", 8),
            Map.entry("august", 8),
            Map.entry("septembre", 9),
            Map.entry("sept", 9),
            Map.entry("september", 9),
            Map.entry("octobre", 10),
            Map.entry("oct", 10),
            Map.entry("october", 10),
            Map.entry("novembre", 11),
            Map.entry("nov", 11),
            Map.entry("november", 11),
            Map.entry("decembre", 12),
            Map.entry("dec", 12),
            Map.entry("december", 12)
    );

    private static final Map<Integer, String> MONTH_LABELS = Map.ofEntries(
            Map.entry(1, "janvier"),
            Map.entry(2, "fevrier"),
            Map.entry(3, "mars"),
            Map.entry(4, "avril"),
            Map.entry(5, "mai"),
            Map.entry(6, "juin"),
            Map.entry(7, "juillet"),
            Map.entry(8, "aout"),
            Map.entry(9, "septembre"),
            Map.entry(10, "octobre"),
            Map.entry(11, "novembre"),
            Map.entry(12, "decembre")
    );

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetTargetService budgetTargetService;
    private final BudgetTargetAlertService budgetTargetAlertService;
    private final ReportService reportService;
    private final UserCardRepository userCardRepository;
    private final RecommendationService recommendationService;
    private final GoalService goalService;
    private final FinancialGoalRepository financialGoalRepository;
    private final ChatContextFormatter formatter;

    public String buildTransactionsContext(Long userId) {
        User user = getRequiredUser(userId);
        return buildTransactionContext(user, detectTimeContext(null));
    }

    public String buildBudgetsContext(Long userId) {
        return buildBudgetContext(getRequiredUser(userId));
    }

    public String buildNotificationsContext(Long userId) {
        return buildBudgetContext(getRequiredUser(userId));
    }

    public String buildReportsContext(Long userId) {
        User user = getRequiredUser(userId);
        return buildMonthlyContext(user, detectTimeContext(null), null);
    }

    public String buildSimulatorsContext(Long userId) {
        return buildSimulationContext(getRequiredUser(userId));
    }

    public String buildCardsContext(Long userId) {
        User user = getRequiredUser(userId);
        return buildCardContext(user, detectTimeContext(null));
    }

    public String buildRecommendationsContext(Long userId) {
        User user = getRequiredUser(userId);
        return buildRecommendationContext(user, loadRecommendationResponse(user));
    }

    public String buildGoalsContext(Long userId) {
        return buildGoalContext(getRequiredUser(userId));
    }

    public String buildGlobalContext(Long userId) {
        User user = getRequiredUser(userId);
        TimeContext timeContext = detectTimeContext(null);
        RecommendationResponseDto recommendationResponse = loadRecommendationResponse(user);

        List<String> sectionNames = new ArrayList<>();
        List<String> sections = new ArrayList<>();
        appendSection(sections, sectionNames, "APP_CONTEXT", () -> buildAppContext(null));
        appendSection(sections, sectionNames, "USER_PROFILE_CONTEXT", () -> buildUserProfileContext(user));
        appendSection(sections, sectionNames, "TRANSACTION_CONTEXT", () -> buildTransactionContext(user, timeContext));
        appendSection(sections, sectionNames, "MONTHLY_CONTEXT", () -> buildMonthlyContext(user, timeContext, summaryOf(recommendationResponse)));
        appendSection(sections, sectionNames, "BUDGET_CONTEXT", () -> buildBudgetContext(user));
        appendSection(sections, sectionNames, "GOAL_CONTEXT", () -> buildGoalContext(user));
        appendSection(sections, sectionNames, "RECOMMENDATION_CONTEXT", () -> buildRecommendationContext(user, recommendationResponse));
        appendSection(sections, sectionNames, "CARD_CONTEXT", () -> buildCardContext(user, timeContext));
        appendSection(sections, sectionNames, "SIMULATION_CONTEXT", () -> buildSimulationContext(user));

        log.info(
                "RAG context built: userId={}, intent={}, detectedPeriods={}, sections={}, transactionCount={}, budgetCount={}, goalCount={}, recommendationCount={}",
                user.getId(),
                RagIntent.UNKNOWN,
                formatDetectedPeriods(timeContext),
                sectionNames,
                transactionRepository.countByUserId(user.getId()),
                getActiveBudgets(user).size(),
                getGoals(user).size(),
                recommendationCount(recommendationResponse)
        );

        return buildContextEnvelope(sections);
    }

    public String buildAdaptiveContext(Long userId, String userMessage) {
        User user = getRequiredUser(userId);
        RagIntent intent = detectIntent(userMessage);
        TimeContext timeContext = detectTimeContext(userMessage);

        List<String> sectionNames = new ArrayList<>();
        List<String> sections = new ArrayList<>();

        RecommendationResponseDto recommendationResponse = requiresRecommendations(intent, userMessage)
                ? loadRecommendationResponse(user)
                : null;

        switch (intent) {
            case APP_EXPLANATION -> {
                appendSection(sections, sectionNames, "APP_CONTEXT", () -> buildAppContext(userMessage));
                if (containsAny(normalize(userMessage), "recommandation", "recommandations", "score", "score financier")) {
                    appendSection(sections, sectionNames, "RECOMMENDATION_CONTEXT", () -> buildRecommendationContext(user, recommendationResponse != null ? recommendationResponse : loadRecommendationResponse(user)));
                } else if (containsAny(normalize(userMessage), "simulateur", "simulateurs", "credit", "epargne")) {
                    appendSection(sections, sectionNames, "SIMULATION_CONTEXT", () -> buildSimulationContext(user));
                } else if (containsAny(normalize(userMessage), "import", "csv", "excel", "transaction")) {
                    appendSection(sections, sectionNames, "TRANSACTION_CONTEXT", () -> buildTransactionContext(user, timeContext));
                } else if (containsAny(normalize(userMessage), "carte", "cartes")) {
                    appendSection(sections, sectionNames, "CARD_CONTEXT", () -> buildCardContext(user, timeContext));
                }
            }
            case TRANSACTIONS -> {
                appendSection(sections, sectionNames, "USER_PROFILE_CONTEXT", () -> buildUserProfileContext(user));
                appendSection(sections, sectionNames, "TRANSACTION_CONTEXT", () -> buildTransactionContext(user, timeContext));
                appendSection(sections, sectionNames, "MONTHLY_CONTEXT", () -> buildMonthlyContext(user, timeContext, summaryOf(recommendationResponse)));
            }
            case MONTHLY_ANALYSIS -> {
                appendSection(sections, sectionNames, "USER_PROFILE_CONTEXT", () -> buildUserProfileContext(user));
                appendSection(sections, sectionNames, "MONTHLY_CONTEXT", () -> buildMonthlyContext(user, timeContext, summaryOf(recommendationResponse)));
                appendSection(sections, sectionNames, "TRANSACTION_CONTEXT", () -> buildTransactionContext(user, timeContext));
            }
            case BUDGETS -> {
                appendSection(sections, sectionNames, "USER_PROFILE_CONTEXT", () -> buildUserProfileContext(user));
                appendSection(sections, sectionNames, "BUDGET_CONTEXT", () -> buildBudgetContext(user));
                appendSection(sections, sectionNames, "TRANSACTION_CONTEXT", () -> buildTransactionContext(user, timeContext));
                appendSection(sections, sectionNames, "MONTHLY_CONTEXT", () -> buildMonthlyContext(user, timeContext, summaryOf(recommendationResponse)));
                appendSection(sections, sectionNames, "RECOMMENDATION_CONTEXT", () -> buildRecommendationContext(user, recommendationResponse != null ? recommendationResponse : loadRecommendationResponse(user)));
            }
            case GOALS -> {
                appendSection(sections, sectionNames, "USER_PROFILE_CONTEXT", () -> buildUserProfileContext(user));
                appendSection(sections, sectionNames, "GOAL_CONTEXT", () -> buildGoalContext(user));
                appendSection(sections, sectionNames, "TRANSACTION_CONTEXT", () -> buildTransactionContext(user, timeContext));
                appendSection(sections, sectionNames, "SIMULATION_CONTEXT", () -> buildSimulationContext(user));
            }
            case RECOMMENDATIONS -> {
                appendSection(sections, sectionNames, "USER_PROFILE_CONTEXT", () -> buildUserProfileContext(user));
                appendSection(sections, sectionNames, "RECOMMENDATION_CONTEXT", () -> buildRecommendationContext(user, recommendationResponse != null ? recommendationResponse : loadRecommendationResponse(user)));
                appendSection(sections, sectionNames, "BUDGET_CONTEXT", () -> buildBudgetContext(user));
                appendSection(sections, sectionNames, "TRANSACTION_CONTEXT", () -> buildTransactionContext(user, timeContext));
                appendSection(sections, sectionNames, "MONTHLY_CONTEXT", () -> buildMonthlyContext(user, timeContext, summaryOf(recommendationResponse)));
            }
            case CARDS -> {
                appendSection(sections, sectionNames, "USER_PROFILE_CONTEXT", () -> buildUserProfileContext(user));
                appendSection(sections, sectionNames, "CARD_CONTEXT", () -> buildCardContext(user, timeContext));
                appendSection(sections, sectionNames, "TRANSACTION_CONTEXT", () -> buildTransactionContext(user, timeContext));
            }
            case SIMULATORS -> {
                appendSection(sections, sectionNames, "APP_CONTEXT", () -> buildAppContext(userMessage));
                appendSection(sections, sectionNames, "SIMULATION_CONTEXT", () -> buildSimulationContext(user));
                appendSection(sections, sectionNames, "GOAL_CONTEXT", () -> buildGoalContext(user));
            }
            case GENERAL_FINANCIAL_ADVICE, UNKNOWN -> {
                appendSection(sections, sectionNames, "APP_CONTEXT", () -> buildAppContext(userMessage));
                appendSection(sections, sectionNames, "USER_PROFILE_CONTEXT", () -> buildUserProfileContext(user));
                appendSection(sections, sectionNames, "TRANSACTION_CONTEXT", () -> buildTransactionContext(user, timeContext));
                appendSection(sections, sectionNames, "BUDGET_CONTEXT", () -> buildBudgetContext(user));
                appendSection(sections, sectionNames, "GOAL_CONTEXT", () -> buildGoalContext(user));
                appendSection(sections, sectionNames, "RECOMMENDATION_CONTEXT", () -> buildRecommendationContext(user, recommendationResponse != null ? recommendationResponse : loadRecommendationResponse(user)));
            }
        }

        log.info(
                "RAG context built: userId={}, intent={}, detectedPeriods={}, sections={}, transactionCount={}, budgetCount={}, goalCount={}, recommendationCount={}",
                user.getId(),
                intent,
                formatDetectedPeriods(timeContext),
                sectionNames,
                sectionsContain(sectionNames, "TRANSACTION_CONTEXT", "MONTHLY_CONTEXT", "CARD_CONTEXT") ? transactionRepository.countByUserId(user.getId()) : -1,
                sectionNames.contains("BUDGET_CONTEXT") ? getActiveBudgets(user).size() : -1,
                sectionNames.contains("GOAL_CONTEXT") || sectionNames.contains("SIMULATION_CONTEXT") ? getGoals(user).size() : -1,
                sectionNames.contains("RECOMMENDATION_CONTEXT") ? recommendationCount(recommendationResponse) : -1
        );

        return buildContextEnvelope(sections);
    }

    private String buildAppContext(String userMessage) {
        List<String> lines = new ArrayList<>();
        lines.add("- Attijari Compass est une application bancaire et PFM intelligente pour suivre les finances personnelles et aider a la decision.");
        lines.add("- Modules principaux: transactions, budgets, cartes, objectifs d'epargne, recommandations IA, reporting mensuel, simulateurs credit/epargne et chatbot IA.");
        lines.add("- Les transactions proviennent d'une source globale unique par utilisateur et peuvent venir de saisies manuelles, imports CSV/Excel et synchronisations cartes.");
        lines.add("- Le module recommandations priorise les actions selon les depenses, les budgets, la severite du mois courant et le potentiel de gain financier.");
        lines.add("- Le score financier est un indicateur applicatif de pilotage qui synthese l'equilibre du mois courant et les signaux de risque detectes.");
        lines.add("- Les simulateurs servent a projeter effort d'epargne, impact credit et scenarios vers un objectif, meme si aucune simulation persistee n'est disponible.");

        String normalizedMessage = normalize(userMessage);
        if (containsAny(normalizedMessage, "import", "csv", "excel")) {
            lines.add("- Import transactions: l'application peut integrer des transactions depuis des fichiers CSV/Excel pour les fusionner au meme espace utilisateur.");
        }
        if (containsAny(normalizedMessage, "score", "score financier")) {
            lines.add("- Quand une question porte sur le score financier, il faut distinguer l'explication du module et les donnees utilisateur reelles eventuellement presentes dans le contexte recommandations.");
        }
        if (containsAny(normalizedMessage, "recommandation", "recommandations")) {
            lines.add("- Le module recommandations transforme les signaux financiers en actions priorisees, avec impact estime et message actionnable.");
        }

        return formatter.buildSection("APP_CONTEXT", lines);
    }

    private String buildUserProfileContext(User user) {
        List<String> lines = new ArrayList<>();
        lines.add("- Utilisateur connecte: " + firstNonBlank(user.getUsername(), user.getEmail(), "Non renseigne"));
        lines.add("- Email: " + firstNonBlank(user.getEmail(), "Non renseigne"));
        lines.add("- Role: " + (user.getRole() != null ? user.getRole().name() : "INCONNU"));
        lines.add("- Date de creation du compte: " + (user.getCreatedAt() != null ? user.getCreatedAt().toString() : "Inconnue"));
        lines.add("- Statut du compte: " + (user.isEnabled() ? "ACTIF" : "INACTIF"));

        return formatter.buildSection("USER_PROFILE_CONTEXT", lines);
    }

    private String buildTransactionContext(User user, TimeContext timeContext) {
        List<Transaction> allTransactions = getAllTransactions(user);
        long totalTransactions = transactionRepository.countByUserId(user.getId());
        Map<TransactionSource, Long> sourceCounts = countBySource(allTransactions);
        Map<TransactionType, Long> typeCounts = countByType(allTransactions);
        Map<TransactionCategory, Double> expenseTotals = aggregateExpenseTotals(allTransactions);

        double totalIncome = sumByType(allTransactions, TransactionType.REVENU);
        double totalExpenses = sumByType(allTransactions, TransactionType.DEPENSE);
        List<Transaction> exampleTransactions = selectTransactionExamples(allTransactions, timeContext);

        List<String> lines = new ArrayList<>();
        lines.add("- Source de verite transactionnelle: table globale des transactions du user connecte, toutes sources confondues.");
        lines.add("- Nombre total de transactions dans l'espace: " + totalTransactions);
        lines.add("- Transactions par source: " + formatSourceCounts(sourceCounts));
        lines.add("- Transactions par type: " + formatTypeCounts(typeCounts));
        lines.add("- Montant total revenus: " + formatMoney(totalIncome) + " DT");
        lines.add("- Montant total depenses: " + formatMoney(totalExpenses) + " DT");
        lines.add("- Solde net global: " + formatMoney(totalIncome - totalExpenses) + " DT");

        String topCategories = formatTopCategories(expenseTotals);
        if (StringUtils.hasText(topCategories)) {
            lines.add("- Top categories de depense globales: " + topCategories);
        }

        if (!timeContext.detectedPeriods().isEmpty()) {
            lines.add("- Periodes detectees dans la question: " + formatDetectedPeriods(timeContext));
        }

        if (!exampleTransactions.isEmpty()) {
            lines.add("- Exemples de transactions recentes (10 maximum, uniquement des exemples et non le total):");
            exampleTransactions.forEach(transaction -> lines.add(String.format(
                    Locale.ROOT,
                    "- %s | %s DT | %s | %s | %s | source=%s",
                    transaction.getDate(),
                    formatMoney(transaction.getAmount()),
                    label(transaction.getCategory()),
                    formatter.truncate(firstNonBlank(transaction.getMerchantName(), transaction.getDescription(), "Transaction"), 64),
                    transaction.getType() != null ? transaction.getType().name() : "N/A",
                    transaction.getSource() != null ? transaction.getSource().name() : "N/A"
            )));
        }

        log.info(
                "Transactions RAG context built: userId={}, totalTransactions={}, sourceCounts={}, typeCounts={}, detectedPeriods={}",
                user.getId(),
                totalTransactions,
                sourceCounts,
                typeCounts,
                formatDetectedPeriods(timeContext)
        );

        return formatter.buildSection("TRANSACTION_CONTEXT", lines);
    }

    private String buildMonthlyContext(User user, TimeContext timeContext, RecommendationSummaryDto summary) {
        List<Transaction> allTransactions = getAllTransactions(user);
        CurrentMonthSnapshot currentMonthSnapshot = buildCurrentMonthSnapshot(user, summary);
        Map<YearMonth, List<Transaction>> historyByMonth = allTransactions.stream()
                .filter(transaction -> transaction.getDate() != null)
                .collect(Collectors.groupingBy(transaction -> YearMonth.from(transaction.getDate())));

        List<String> lines = new ArrayList<>();
        lines.add("- Source de verite du mois courant: utiliser les montants canoniques ci-dessous si la question porte sur le mois courant.");
        lines.add("- Mois courant canonique: " + currentMonthSnapshot.monthLabel());
        lines.add("- Revenus du mois courant: " + formatMoney(currentMonthSnapshot.monthlyIncome()) + " DT");
        lines.add("- Depenses du mois courant: " + formatMoney(currentMonthSnapshot.monthlyExpenses()) + " DT");
        lines.add("- Solde net du mois courant: " + formatMoney(currentMonthSnapshot.netBalance()) + " DT");
        lines.add("- Taux d'epargne du mois courant: " + formatMoney(currentMonthSnapshot.savingsRate()) + "%");
        lines.add("- Severite du mois courant: " + currentMonthSnapshot.currentMonthSeverity());
        lines.add("- Transactions sur le mois courant: " + currentMonthSnapshot.transactionCount());

        if (currentMonthSnapshot.financialScore() != null || StringUtils.hasText(currentMonthSnapshot.financialScoreLabel())) {
            lines.add(String.format(
                    Locale.ROOT,
                    "- Score financier du mois courant: %s | label %s",
                    firstNonBlank(currentMonthSnapshot.financialScore() != null ? currentMonthSnapshot.financialScore().toString() : null, "N/A"),
                    firstNonBlank(currentMonthSnapshot.financialScoreLabel(), "N/A")
            ));
        }

        if (!timeContext.detectedPeriods().isEmpty()) {
            lines.add("- Periodes detectees pour l'analyse: " + formatDetectedPeriods(timeContext));
            for (PeriodWindow period : timeContext.detectedPeriods()) {
                List<Transaction> periodTransactions = filterTransactionsByWindow(allTransactions, period.startDate(), period.endDate());
                double periodIncome = sumByType(periodTransactions, TransactionType.REVENU);
                double periodExpenses = sumByType(periodTransactions, TransactionType.DEPENSE);
                Map<TransactionCategory, Double> periodTopCategories = aggregateExpenseTotals(periodTransactions);

                lines.add(String.format(
                        Locale.ROOT,
                        "- %s | transactions %d | revenus %s DT | depenses %s DT | solde net %s DT | top depenses %s",
                        period.label(),
                        periodTransactions.size(),
                        formatMoney(periodIncome),
                        formatMoney(periodExpenses),
                        formatMoney(periodIncome - periodExpenses),
                        firstNonBlank(formatTopCategories(periodTopCategories), "Aucune categorie dominante")
                ));
            }

            if (timeContext.compareMode() && timeContext.detectedPeriods().size() >= 2) {
                PeriodWindow first = timeContext.detectedPeriods().get(0);
                PeriodWindow second = timeContext.detectedPeriods().get(1);
                MonthlyMetrics firstMetrics = computeMonthlyMetrics(allTransactions, first);
                MonthlyMetrics secondMetrics = computeMonthlyMetrics(allTransactions, second);

                lines.add(String.format(
                        Locale.ROOT,
                        "- Comparaison %s vs %s | depenses %s DT vs %s DT | variation %s DT | revenus %s DT vs %s DT | solde net %s DT vs %s DT",
                        first.label(),
                        second.label(),
                        formatMoney(firstMetrics.expenses()),
                        formatMoney(secondMetrics.expenses()),
                        formatMoney(secondMetrics.expenses() - firstMetrics.expenses()),
                        formatMoney(firstMetrics.income()),
                        formatMoney(secondMetrics.income()),
                        formatMoney(firstMetrics.netBalance()),
                        formatMoney(secondMetrics.netBalance())
                ));
            }
        }

        List<YearMonth> latestMonths = historyByMonth.keySet().stream()
                .sorted(Comparator.reverseOrder())
                .limit(6)
                .toList();

        if (!latestMonths.isEmpty()) {
            lines.add("- Historique mensuel disponible:");
            latestMonths.forEach(month -> {
                MonthlyMetrics metrics = computeMonthlyMetrics(allTransactions, month);
                lines.add(String.format(
                        Locale.ROOT,
                        "- %s | transactions %d | revenus %s DT | depenses %s DT | solde net %s DT",
                        monthLabel(month),
                        metrics.transactionCount(),
                        formatMoney(metrics.income()),
                        formatMoney(metrics.expenses()),
                        formatMoney(metrics.netBalance())
                ));
            });
        }

        log.info(
                "Monthly RAG context built: userId={}, detectedPeriods={}, availableHistoryMonths={}",
                user.getId(),
                formatDetectedPeriods(timeContext),
                latestMonths.size()
        );

        return formatter.buildSection("MONTHLY_CONTEXT", lines);
    }

    private String buildBudgetContext(User user) {
        List<BudgetTargetResponse> budgets = getActiveBudgets(user);
        List<BudgetAlertResponse> alerts = getBudgetAlerts(user);

        List<String> lines = new ArrayList<>();
        lines.add("- Nombre de budgets actifs: " + budgets.size());

        budgets.stream()
                .sorted(Comparator.comparing(BudgetTargetResponse::getUsagePercent, Comparator.nullsLast(BigDecimal::compareTo)).reversed())
                .limit(6)
                .forEach(budget -> lines.add(String.format(
                        Locale.ROOT,
                        "- Budget %s | cible %s DT | depense %s DT | reste %s DT | usage %s%% | statut %s",
                        firstNonBlank(budget.getCategoryLabel(), label(budget.getCategory())),
                        formatMoney(budget.getTargetAmount()),
                        formatMoney(budget.getSpentThisMonth()),
                        formatMoney(budget.getRemainingAmount()),
                        formatMoney(budget.getUsagePercent()),
                        firstNonBlank(budget.getMonitoringStatusLabel(), "Sans statut")
                )));

        List<String> exceeded = budgets.stream()
                .filter(budget -> budget.getUsagePercent() != null && budget.getUsagePercent().doubleValue() > 100d)
                .limit(4)
                .map(budget -> firstNonBlank(budget.getCategoryLabel(), label(budget.getCategory())))
                .toList();
        if (!exceeded.isEmpty()) {
            lines.add("- Budgets depasses: " + String.join(", ", exceeded));
        }

        List<String> critical = budgets.stream()
                .filter(budget -> StringUtils.hasText(budget.getMonitoringStatusLabel()) && normalize(budget.getMonitoringStatusLabel()).contains("crit"))
                .limit(4)
                .map(budget -> firstNonBlank(budget.getCategoryLabel(), label(budget.getCategory())))
                .toList();
        if (!critical.isEmpty()) {
            lines.add("- Budgets critiques: " + String.join(", ", critical));
        }

        if (!alerts.isEmpty()) {
            lines.add("- Alertes budget prioritaires:");
            alerts.stream()
                    .limit(4)
                    .forEach(alert -> lines.add(String.format(
                            Locale.ROOT,
                            "- %s | %s | %s",
                            firstNonBlank(alert.getSeverityLabel(), "Info"),
                            firstNonBlank(alert.getTitle(), "Alerte"),
                            formatter.truncate(firstNonBlank(alert.getMessage(), "Aucune precision"), 140)
                    )));
        }

        log.info(
                "Budget RAG context built: userId={}, budgetCount={}, alertCount={}",
                user.getId(),
                budgets.size(),
                alerts.size()
        );

        return formatter.buildSection("BUDGET_CONTEXT", lines);
    }

    private String buildGoalContext(User user) {
        List<GoalResponse> goals = getGoals(user);
        long inProgressGoals = financialGoalRepository.countByUserIdAndStatus(user.getId(), GoalStatus.EN_COURS);
        long achievedGoals = financialGoalRepository.countByUserIdAndStatus(user.getId(), GoalStatus.ATTEINT);
        LocalDate today = LocalDate.now();

        List<String> lines = new ArrayList<>();
        lines.add("- Nombre total d'objectifs financiers: " + goals.size());
        lines.add("- Objectifs en cours: " + inProgressGoals);
        lines.add("- Objectifs atteints: " + achievedGoals);

        goals.stream()
                .limit(5)
                .forEach(goal -> lines.add(String.format(
                        Locale.ROOT,
                        "- Objectif %s | progression %s%% | cible %s DT | actuel %s DT | reste %s DT | echeance %s | effort mensuel %s DT",
                        firstNonBlank(goal.getName(), "Objectif"),
                        formatMoney(goal.getProgressPercentage()),
                        formatMoney(goal.getTargetAmount()),
                        formatMoney(goal.getCurrentAmount()),
                        formatMoney(goal.getRemainingAmount()),
                        goal.getTargetDate(),
                        formatMoney(goal.getMonthlySavingsRequired())
                )));

        List<String> lateGoals = goals.stream()
                .filter(goal -> goal.getTargetDate() != null
                        && goal.getTargetDate().isBefore(today)
                        && (goal.getProgressPercentage() == null || goal.getProgressPercentage() < 100d))
                .limit(3)
                .map(goal -> firstNonBlank(goal.getName(), "Objectif"))
                .toList();
        if (!lateGoals.isEmpty()) {
            lines.add("- Objectifs en retard: " + String.join(", ", lateGoals));
        }

        List<GoalAiRefreshResponse> goalSnapshots = goals.stream()
                .limit(3)
                .map(goal -> safeGoalSnapshot(goal.getId(), user.getEmail()))
                .filter(snapshot -> snapshot != null)
                .toList();

        if (!goalSnapshots.isEmpty()) {
            lines.add("- Faisabilite et scenarios disponibles:");
            goalSnapshots.forEach(snapshot -> lines.add(String.format(
                    Locale.ROOT,
                    "- %s | faisabilite %s | probabilite %s%% | risque %s | atteignable a date cible %s | capacite equilibree %s DT",
                    firstNonBlank(snapshot.getName(), "Objectif"),
                    formatMoney(snapshot.getFeasibilityScore()),
                    formatMoney(snapshot.getSuccessProbability()),
                    firstNonBlank(snapshot.getRiskLevel(), "N/A"),
                    Boolean.TRUE.equals(snapshot.getAchievableByTargetDate()) ? "oui" : "non",
                    formatMoney(snapshot.getBalancedCapacity())
            )));
        }

        log.info(
                "Goal RAG context built: userId={}, goalCount={}, goalAiSnapshots={}",
                user.getId(),
                goals.size(),
                goalSnapshots.size()
        );

        return formatter.buildSection("GOAL_CONTEXT", lines);
    }

    private String buildRecommendationContext(User user, RecommendationResponseDto response) {
        RecommendationSummaryDto summary = summaryOf(response);
        List<RecommendationDto> recommendations = response != null && response.getRecommendations() != null
                ? response.getRecommendations()
                : List.of();

        List<String> lines = new ArrayList<>();
        lines.add("- Module recommandations: priorisation d'actions financieres selon les signaux transactions, budgets, objectifs et severite du mois courant.");

        if (summary != null) {
            lines.add(String.format(
                    Locale.ROOT,
                    "- Score financier %s | label %s | statut global %s | recommandations %s | gain mensuel estime %s DT",
                    firstNonBlank(summary.getFinancialScore() != null ? summary.getFinancialScore().toString() : null, "N/A"),
                    firstNonBlank(summary.getFinancialScoreLabel(), "N/A"),
                    firstNonBlank(summary.getGlobalStatus(), "N/A"),
                    firstNonBlank(summary.getTotalRecommendations() != null ? summary.getTotalRecommendations().toString() : null, "0"),
                    formatMoney(summary.getTotalEstimatedMonthlyGain())
            ));
            lines.add("- Severite du mois courant selon le module: " + (summary.getCurrentMonthSeverity() != null ? summary.getCurrentMonthSeverity().name() : "N/A"));
            if (StringUtils.hasText(summary.getAiSummary())) {
                lines.add("- Synthese IA: " + formatter.truncate(summary.getAiSummary(), 180));
            }
        }

        recommendations.stream()
                .limit(5)
                .forEach(recommendation -> lines.add(String.format(
                        Locale.ROOT,
                        "- %s | priorite %s | source %s | impact %s DT | action %s",
                        firstNonBlank(recommendation.getTitle(), "Recommendation"),
                        recommendation.getPriority() != null ? recommendation.getPriority().name() : "N/A",
                        firstNonBlank(recommendation.getSourceType(), "N/A"),
                        formatMoney(recommendation.getEstimatedMonthlyGain()),
                        formatter.truncate(firstNonBlank(recommendation.getSuggestedAction(), recommendation.getMessage(), "Aucune action precise"), 100)
                )));

        if (response != null && response.getStorytelling() != null && StringUtils.hasText(response.getStorytelling().getAction())) {
            lines.add("- Action recommandee principale: " + formatter.truncate(response.getStorytelling().getAction(), 150));
        }

        log.info(
                "Recommendation RAG context built: userId={}, recommendationCount={}, hasSummary={}",
                user.getId(),
                recommendations.size(),
                summary != null
        );

        return formatter.buildSection("RECOMMENDATION_CONTEXT", lines);
    }

    private String buildCardContext(User user, TimeContext timeContext) {
        List<UserCard> cards = userCardRepository.findAllByUserIdOrderByConnectedAtDesc(user.getId());
        List<Transaction> baseTransactions = !timeContext.detectedPeriods().isEmpty()
                ? filterTransactionsByPeriods(getAllTransactions(user), timeContext.detectedPeriods())
                : getAllTransactions(user);

        List<Transaction> cardTransactions = baseTransactions.stream()
                .filter(this::isCardTransaction)
                .sorted(Comparator.comparing(Transaction::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

        Map<String, Long> usageCount = cardTransactions.stream()
                .map(Transaction::getCardLast4)
                .filter(StringUtils::hasText)
                .collect(Collectors.groupingBy(last4 -> last4, Collectors.counting()));

        List<String> lines = new ArrayList<>();
        lines.add("- Nombre de cartes connectees: " + cards.size());

        cards.stream().limit(5).forEach(card -> lines.add(String.format(
                Locale.ROOT,
                "- Carte %s | banque %s | statut %s | source %s | derniere sync %s",
                firstNonBlank(card.getMaskedCardNumber(), maskFromLast4(card.getLast4()), "Carte"),
                firstNonBlank(card.getBankName(), "Banque non renseignee"),
                card.getStatus() != null ? card.getStatus().name() : "INCONNU",
                card.getSourceType() != null ? card.getSourceType().name() : "INCONNUE",
                card.getLastSyncAt() != null ? card.getLastSyncAt().toString() : "Jamais"
        )));

        lines.add("- Transactions carte disponibles dans le contexte: " + cardTransactions.size());
        usageCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(entry -> lines.add("- Carte la plus utilisee: **** " + entry.getKey() + " (" + entry.getValue() + " transactions)"));

        cardTransactions.stream().limit(5).forEach(transaction -> lines.add(String.format(
                Locale.ROOT,
                "- Transaction carte %s | %s DT | %s | %s",
                firstNonBlank(transaction.getCardLast4(), "N/A"),
                formatMoney(transaction.getAmount()),
                label(transaction.getCategory()),
                formatter.truncate(firstNonBlank(transaction.getMerchantName(), transaction.getDescription(), "Transaction"), 52)
        )));

        log.info(
                "Card RAG context built: userId={}, cardCount={}, cardTransactionCount={}, detectedPeriods={}",
                user.getId(),
                cards.size(),
                cardTransactions.size(),
                formatDetectedPeriods(timeContext)
        );

        return formatter.buildSection("CARD_CONTEXT", lines);
    }

    private String buildSimulationContext(User user) {
        List<GoalResponse> goals = getGoals(user);

        List<String> lines = new ArrayList<>();
        lines.add("- Les simulateurs Attijari Compass servent a projeter l'effort d'epargne, la capacite de credit et plusieurs scenarios d'atteinte d'objectif.");
        lines.add("- Meme sans historique de simulation persiste, ce module explique l'impact d'un effort mensuel plus eleve, d'un horizon plus long ou d'un arbitrage de depenses.");
        lines.add("- Le simulateur epargne aide a estimer le montant mensuel a mettre de cote pour atteindre une cible a une date donnee.");
        lines.add("- Le simulateur credit aide a comprendre le poids d'une mensualite, la duree et l'equilibre budgetaire necessaire.");

        if (!goals.isEmpty()) {
            goals.stream()
                    .limit(3)
                    .forEach(goal -> lines.add(String.format(
                            Locale.ROOT,
                            "- Cas utilisateur %s | reste %s DT | effort mensuel estime %s DT",
                            firstNonBlank(goal.getName(), "Objectif"),
                            formatMoney(goal.getRemainingAmount()),
                            formatMoney(goal.getMonthlySavingsRequired())
                    )));
        } else {
            lines.add("- Aucun objectif utilisateur n'est disponible pour illustrer une simulation personnalisee.");
        }

        log.info("Simulation RAG context built: userId={}, goalCount={}", user.getId(), goals.size());
        return formatter.buildSection("SIMULATION_CONTEXT", lines);
    }

    private RecommendationResponseDto loadRecommendationResponse(User user) {
        try {
            return recommendationService.generateRecommendationsForUser(user.getId());
        } catch (Exception ex) {
            log.warn("Recommendation summary unavailable for chat context: userId={}, message={}", user.getId(), ex.getMessage());
            return null;
        }
    }

    private CurrentMonthSnapshot buildCurrentMonthSnapshot(User user, RecommendationSummaryDto summary) {
        List<Transaction> currentMonthTransactions = getCurrentMonthTransactions(user);
        boolean missingCanonicalSummary = summary == null
                || summary.getCurrentMonthIncome() == null
                || summary.getCurrentMonthExpenses() == null
                || summary.getCurrentMonthNetBalance() == null
                || summary.getCurrentMonthSavingsRate() == null;

        ReportSummaryResponse reportSummary = missingCanonicalSummary
                ? reportService.getSummary(user.getEmail(), null)
                : null;

        String monthLabel = reportSummary != null
                ? firstNonBlank(reportSummary.getMonthLabel(), reportSummary.getMonth(), monthLabel(YearMonth.now()))
                : monthLabel(YearMonth.now());

        return new CurrentMonthSnapshot(
                monthLabel,
                firstNonNull(summary != null ? summary.getCurrentMonthIncome() : null, reportSummary != null ? reportSummary.getIncome() : null),
                firstNonNull(summary != null ? summary.getCurrentMonthExpenses() : null, reportSummary != null ? reportSummary.getExpenses() : null),
                firstNonNull(summary != null ? summary.getCurrentMonthNetBalance() : null, reportSummary != null ? reportSummary.getNetBalance() : null),
                firstNonNull(summary != null ? summary.getCurrentMonthSavingsRate() : null, reportSummary != null ? reportSummary.getSavingsRate() : null),
                summary != null && summary.getCurrentMonthSeverity() != null ? summary.getCurrentMonthSeverity().name() : "NORMAL",
                summary != null ? summary.getFinancialScore() : null,
                summary != null ? summary.getFinancialScoreLabel() : null,
                currentMonthTransactions.size()
        );
    }

    private RagIntent detectIntent(String question) {
        String normalized = normalize(question);
        if (!StringUtils.hasText(normalized)) {
            return RagIntent.UNKNOWN;
        }

        if (containsAny(normalized, "attijari compass", "c est quoi", "a quoi sert", "comment fonctionne", "comment importer", "importer", "application", "projet", "module")) {
            return RagIntent.APP_EXPLANATION;
        }
        if (containsAny(normalized, "budget", "budgets", "depasse", "depassement", "critique", "usage")) {
            return RagIntent.BUDGETS;
        }
        if (containsAny(normalized, "objectif", "objectifs", "goal", "atteindre", "retard", "prioritaire")) {
            return RagIntent.GOALS;
        }
        if (containsAny(normalized, "recommandation", "recommandations", "score financier", "score", "action recommandee")) {
            return RagIntent.RECOMMENDATIONS;
        }
        if (containsAny(normalized, "carte", "cartes", "cb", "card", "synchronis")) {
            return RagIntent.CARDS;
        }
        if (containsAny(normalized, "simulateur", "simulateurs", "credit", "epargne", "simulation", "simulations")) {
            return RagIntent.SIMULATORS;
        }
        if (containsAny(normalized, "mois", "janvier", "fevrier", "mars", "avril", "mai", "juin", "juillet", "aout", "septembre", "octobre", "novembre", "decembre", "compare", "comparaison", "annee", "annee 20")) {
            return RagIntent.MONTHLY_ANALYSIS;
        }
        if (containsAny(normalized, "transaction", "transactions", "solde net", "revenu", "revenus", "depense", "depenses", "categorie")) {
            return RagIntent.TRANSACTIONS;
        }
        if (containsAny(normalized, "conseil", "conseils", "ameliorer", "optimiser", "finance", "financier")) {
            return RagIntent.GENERAL_FINANCIAL_ADVICE;
        }
        return RagIntent.UNKNOWN;
    }

    private TimeContext detectTimeContext(String question) {
        String normalized = normalize(question);
        if (!StringUtils.hasText(normalized)) {
            return TimeContext.empty();
        }

        Set<PeriodWindow> periods = new LinkedHashSet<>();
        Integer explicitYear = extractYear(normalized);
        YearMonth now = YearMonth.now();
        boolean compareMode = containsAny(normalized, "compare", "comparaison", "versus", "vs");

        if (containsAny(normalized, "ce mois", "ce mois ci", "mois courant", "this month")) {
            periods.add(periodForMonth(now));
        }

        if (containsAny(normalized, "mois dernier", "last month")) {
            periods.add(periodForMonth(now.minusMonths(1)));
        }

        for (Map.Entry<String, Integer> entry : MONTH_TOKENS.entrySet()) {
            if (containsWord(normalized, entry.getKey())) {
                int year = explicitYear != null ? explicitYear : now.getYear();
                periods.add(periodForMonth(YearMonth.of(year, entry.getValue())));
            }
        }

        if (explicitYear != null && periods.isEmpty() && containsAny(normalized, "annee", "year")) {
            periods.add(periodForYear(explicitYear));
        }

        if (periods.size() == 1
                && containsAny(normalized, "augmente", "augmenter", "augmentation", "baisse", "diminue", "diminution", "evolution")) {
            PeriodWindow first = periods.iterator().next();
            if (first.yearMonth() != null) {
                periods.add(periodForMonth(first.yearMonth().minusMonths(1)));
                compareMode = true;
            }
        }

        List<PeriodWindow> detectedPeriods = periods.stream().toList();
        return new TimeContext(detectedPeriods, compareMode, explicitYear);
    }

    private List<Transaction> getCurrentMonthTransactions(User user) {
        LocalDate now = LocalDate.now();
        return transactionRepository.findAllByUserIdAndDateBetweenOrderByDateDescCreatedAtDesc(
                user.getId(),
                now.withDayOfMonth(1),
                now.withDayOfMonth(now.lengthOfMonth())
        );
    }

    private List<Transaction> getAllTransactions(User user) {
        return transactionRepository.findAllByUserId(user.getId());
    }

    private List<BudgetTargetResponse> getActiveBudgets(User user) {
        return budgetTargetService.getActiveBudgetTargetsForCurrentUser(user.getEmail());
    }

    private List<BudgetAlertResponse> getBudgetAlerts(User user) {
        return budgetTargetAlertService.getAlertsForCurrentUser(user.getEmail());
    }

    private List<GoalResponse> getGoals(User user) {
        return goalService.getAllGoals(user.getEmail());
    }

    private GoalAiRefreshResponse safeGoalSnapshot(Long goalId, String email) {
        try {
            return goalService.getGoalAiRefresh(goalId, email);
        } catch (Exception ex) {
            log.warn("Goal AI snapshot unavailable for chat context: goalId={}, message={}", goalId, ex.getMessage());
            return null;
        }
    }

    private List<Transaction> selectTransactionExamples(List<Transaction> allTransactions, TimeContext timeContext) {
        List<Transaction> baseTransactions = !timeContext.detectedPeriods().isEmpty()
                ? filterTransactionsByPeriods(allTransactions, timeContext.detectedPeriods())
                : allTransactions;

        return baseTransactions.stream()
                .sorted(Comparator
                        .comparing(Transaction::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed()
                        .thenComparing(Transaction::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_TRANSACTION_EXAMPLES)
                .toList();
    }

    private List<Transaction> filterTransactionsByPeriods(List<Transaction> transactions, List<PeriodWindow> periods) {
        if (periods == null || periods.isEmpty()) {
            return transactions;
        }

        return transactions.stream()
                .filter(transaction -> periods.stream().anyMatch(period -> isWithinWindow(transaction, period.startDate(), period.endDate())))
                .toList();
    }

    private List<Transaction> filterTransactionsByWindow(List<Transaction> transactions, LocalDate startDate, LocalDate endDate) {
        return transactions.stream()
                .filter(transaction -> isWithinWindow(transaction, startDate, endDate))
                .toList();
    }

    private boolean isWithinWindow(Transaction transaction, LocalDate startDate, LocalDate endDate) {
        return transaction.getDate() != null
                && !transaction.getDate().isBefore(startDate)
                && !transaction.getDate().isAfter(endDate);
    }

    private MonthlyMetrics computeMonthlyMetrics(List<Transaction> allTransactions, YearMonth month) {
        return computeMonthlyMetrics(allTransactions, periodForMonth(month));
    }

    private MonthlyMetrics computeMonthlyMetrics(List<Transaction> allTransactions, PeriodWindow period) {
        List<Transaction> periodTransactions = filterTransactionsByWindow(allTransactions, period.startDate(), period.endDate());
        double income = sumByType(periodTransactions, TransactionType.REVENU);
        double expenses = sumByType(periodTransactions, TransactionType.DEPENSE);
        return new MonthlyMetrics(
                periodTransactions.size(),
                income,
                expenses,
                income - expenses
        );
    }

    private Map<TransactionSource, Long> countBySource(List<Transaction> transactions) {
        Map<TransactionSource, Long> counts = new EnumMap<>(TransactionSource.class);
        for (Transaction transaction : transactions) {
            TransactionSource source = transaction.getSource();
            if (source != null) {
                counts.merge(source, 1L, Long::sum);
            }
        }
        return counts;
    }

    private Map<TransactionType, Long> countByType(List<Transaction> transactions) {
        Map<TransactionType, Long> counts = new EnumMap<>(TransactionType.class);
        for (Transaction transaction : transactions) {
            TransactionType type = transaction.getType();
            if (type != null) {
                counts.merge(type, 1L, Long::sum);
            }
        }
        return counts;
    }

    private Map<TransactionCategory, Double> aggregateExpenseTotals(List<Transaction> transactions) {
        Map<TransactionCategory, Double> expenseTotals = new EnumMap<>(TransactionCategory.class);
        for (Transaction transaction : transactions) {
            if (transaction.getType() != TransactionType.DEPENSE || transaction.getCategory() == null) {
                continue;
            }

            double amount = Math.abs(transaction.getAmount() != null ? transaction.getAmount() : 0d);
            if (amount > 0d) {
                expenseTotals.merge(transaction.getCategory(), amount, Double::sum);
            }
        }
        return expenseTotals;
    }

    private double sumByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .map(Transaction::getAmount)
                .filter(amount -> amount != null && amount > 0d)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    private boolean requiresRecommendations(RagIntent intent, String userMessage) {
        return switch (intent) {
            case BUDGETS, RECOMMENDATIONS, GENERAL_FINANCIAL_ADVICE, UNKNOWN -> true;
            case APP_EXPLANATION -> containsAny(normalize(userMessage), "recommandation", "recommandations", "score", "score financier");
            default -> false;
        };
    }

    private RecommendationSummaryDto summaryOf(RecommendationResponseDto response) {
        return response != null ? response.getSummary() : null;
    }

    private int recommendationCount(RecommendationResponseDto response) {
        return response != null && response.getRecommendations() != null ? response.getRecommendations().size() : 0;
    }

    private void appendSection(List<String> sections, List<String> sectionNames, String sectionName, Supplier<String> supplier) {
        String section = safeSection(supplier);
        if (StringUtils.hasText(section)) {
            sections.add(section);
            sectionNames.add(sectionName);
        }
    }

    private String safeSection(Supplier<String> supplier) {
        try {
            return supplier.get();
        } catch (Exception ex) {
            log.warn("RAG section skipped: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private String buildContextEnvelope(List<String> sections) {
        String body = sections.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));

        return formatter.truncate("=== CONTEXTE RAG ATTIJARI COMPASS ===" + System.lineSeparator()
                + System.lineSeparator()
                + body
                + System.lineSeparator()
                + System.lineSeparator()
                + "=== FIN CONTEXTE ===", MAX_CONTEXT_LENGTH);
    }

    private User getRequiredUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private boolean containsAny(String normalizedValue, String... tokens) {
        for (String token : tokens) {
            if (normalizedValue.contains(normalize(token))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsWord(String normalizedValue, String token) {
        return Pattern.compile("\\b" + Pattern.quote(normalize(token)) + "\\b").matcher(normalizedValue).find();
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private Integer extractYear(String normalized) {
        Matcher matcher = YEAR_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private PeriodWindow periodForMonth(YearMonth yearMonth) {
        return new PeriodWindow(
                yearMonth.atDay(1),
                yearMonth.atEndOfMonth(),
                monthLabel(yearMonth),
                yearMonth
        );
    }

    private PeriodWindow periodForYear(int year) {
        return new PeriodWindow(
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31),
                "annee " + year,
                null
        );
    }

    private String formatDetectedPeriods(TimeContext timeContext) {
        if (timeContext == null || timeContext.detectedPeriods().isEmpty()) {
            return "ALL_TIME";
        }

        return timeContext.detectedPeriods().stream()
                .map(PeriodWindow::label)
                .collect(Collectors.joining(" | "));
    }

    private String monthLabel(YearMonth yearMonth) {
        return firstNonBlank(MONTH_LABELS.get(yearMonth.getMonthValue()), yearMonth.getMonth().name().toLowerCase(Locale.ROOT))
                + " "
                + yearMonth.getYear();
    }

    private boolean sectionsContain(Collection<String> sections, String... expected) {
        for (String section : expected) {
            if (sections.contains(section)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCardTransaction(Transaction transaction) {
        return transaction.getPaymentMethod() == PaymentMethod.CARD
                || StringUtils.hasText(transaction.getCardLast4())
                || transaction.getSource() == TransactionSource.CARD_SYNC
                || transaction.getSource() == TransactionSource.MANUAL_CARD
                || transaction.getSource() == TransactionSource.CARD_SANDBOX
                || transaction.getSource() == TransactionSource.TEST_CARD;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private Double firstNonNull(Double... values) {
        for (Double value : values) {
            if (value != null) {
                return value;
            }
        }
        return 0d;
    }

    private String label(TransactionCategory category) {
        return category != null ? category.label() : TransactionCategory.fallback().label();
    }

    private String sourceLabel(TransactionSource source) {
        if (source == null) {
            return "Inconnue";
        }

        return switch (source) {
            case MANUAL_ENTRY -> "Manuelles";
            case IMPORTED_FILE -> "Importees";
            case CARD_SYNC -> "Synchronisees carte";
            case MANUAL_CARD -> "Cartes manuelles";
            case CARD_SANDBOX -> "Cartes sandbox";
            case BANK_API -> "Banque API";
            case TEST_CARD -> "Cartes test";
        };
    }

    private String typeLabel(TransactionType type) {
        if (type == null) {
            return "Inconnu";
        }

        return switch (type) {
            case REVENU -> "Revenus";
            case DEPENSE -> "Depenses";
        };
    }

    private String formatSourceCounts(Map<TransactionSource, Long> sourceCounts) {
        if (sourceCounts.isEmpty()) {
            return "aucune transaction";
        }

        return sourceCounts.entrySet().stream()
                .sorted(Map.Entry.<TransactionSource, Long>comparingByValue().reversed())
                .map(entry -> sourceLabel(entry.getKey()) + ": " + entry.getValue())
                .collect(Collectors.joining(" | "));
    }

    private String formatTypeCounts(Map<TransactionType, Long> typeCounts) {
        if (typeCounts.isEmpty()) {
            return "aucune transaction";
        }

        return typeCounts.entrySet().stream()
                .sorted(Map.Entry.<TransactionType, Long>comparingByKey())
                .map(entry -> typeLabel(entry.getKey()) + ": " + entry.getValue())
                .collect(Collectors.joining(" | "));
    }

    private String formatTopCategories(Map<TransactionCategory, Double> expenseTotals) {
        if (expenseTotals.isEmpty()) {
            return "";
        }

        return expenseTotals.entrySet().stream()
                .sorted(Map.Entry.<TransactionCategory, Double>comparingByValue().reversed())
                .limit(5)
                .map(entry -> label(entry.getKey()) + ": " + formatMoney(entry.getValue()) + " DT")
                .collect(Collectors.joining(" | "));
    }

    private String formatMoney(Number value) {
        if (value == null) {
            return "0";
        }

        double normalized = Math.round(value.doubleValue() * 100d) / 100d;
        if (Math.abs(normalized - Math.rint(normalized)) < 0.0001d) {
            return String.format(Locale.ROOT, "%.0f", normalized);
        }
        return String.format(Locale.ROOT, "%.2f", normalized);
    }

    private String maskFromLast4(String last4) {
        return StringUtils.hasText(last4) ? "**** " + last4 : null;
    }

    private enum RagIntent {
        TRANSACTIONS,
        MONTHLY_ANALYSIS,
        BUDGETS,
        GOALS,
        RECOMMENDATIONS,
        CARDS,
        SIMULATORS,
        APP_EXPLANATION,
        GENERAL_FINANCIAL_ADVICE,
        UNKNOWN
    }

    private record CurrentMonthSnapshot(
            String monthLabel,
            Double monthlyIncome,
            Double monthlyExpenses,
            Double netBalance,
            Double savingsRate,
            String currentMonthSeverity,
            Integer financialScore,
            String financialScoreLabel,
            int transactionCount
    ) {
    }

    private record MonthlyMetrics(
            int transactionCount,
            double income,
            double expenses,
            double netBalance
    ) {
    }

    private record PeriodWindow(
            LocalDate startDate,
            LocalDate endDate,
            String label,
            YearMonth yearMonth
    ) {
    }

    private record TimeContext(
            List<PeriodWindow> detectedPeriods,
            boolean compareMode,
            Integer explicitYear
    ) {
        private static TimeContext empty() {
            return new TimeContext(List.of(), false, null);
        }
    }
}

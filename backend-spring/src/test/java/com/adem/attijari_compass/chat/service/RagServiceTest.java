package com.adem.attijari_compass.chat.service;

import com.adem.attijari_compass.entity.Role;
import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.recommendation.dto.RecommendationResponseDto;
import com.adem.attijari_compass.recommendation.dto.RecommendationSummaryDto;
import com.adem.attijari_compass.recommendation.enums.CurrentMonthSeverity;
import com.adem.attijari_compass.recommendation.service.RecommendationService;
import com.adem.attijari_compass.repository.FinancialGoalRepository;
import com.adem.attijari_compass.repository.TransactionRepository;
import com.adem.attijari_compass.repository.UserCardRepository;
import com.adem.attijari_compass.repository.UserRepository;
import com.adem.attijari_compass.service.BudgetTargetAlertService;
import com.adem.attijari_compass.service.BudgetTargetService;
import com.adem.attijari_compass.service.GoalService;
import com.adem.attijari_compass.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private BudgetTargetService budgetTargetService;
    @Mock
    private BudgetTargetAlertService budgetTargetAlertService;
    @Mock
    private ReportService reportService;
    @Mock
    private UserCardRepository userCardRepository;
    @Mock
    private RecommendationService recommendationService;
    @Mock
    private GoalService goalService;
    @Mock
    private FinancialGoalRepository financialGoalRepository;

    private RagService ragService;

    @BeforeEach
    void setUp() {
        ragService = new RagService(
                userRepository,
                transactionRepository,
                budgetTargetService,
                budgetTargetAlertService,
                reportService,
                userCardRepository,
                recommendationService,
                goalService,
                financialGoalRepository,
                new ChatContextFormatter()
        );
    }

    @Test
    void buildAdaptiveContext_prioritizesCanonicalMonthlySummary() {
        User user = sampleUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(recommendationService.generateRecommendationsForUser(1L)).thenReturn(RecommendationResponseDto.builder()
                .summary(RecommendationSummaryDto.builder()
                        .financialScore(72)
                        .financialScoreLabel("A surveiller")
                        .currentMonthSeverity(CurrentMonthSeverity.NORMAL)
                        .currentMonthIncome(22602.99)
                        .currentMonthExpenses(21000.00)
                        .currentMonthNetBalance(1602.99)
                        .currentMonthSavingsRate(7.09)
                        .totalRecommendations(2)
                        .totalEstimatedMonthlyGain(340.0)
                        .build())
                .recommendations(List.of())
                .build());
        when(transactionRepository.findAllByUserId(1L)).thenReturn(List.of(
                transaction(LocalDate.now(), 22602.99, TransactionType.REVENU, TransactionCategory.SALAIRE, "Salaire avril"),
                transaction(LocalDate.now().minusDays(2), 21000.0, TransactionType.DEPENSE, TransactionCategory.AUTRES, "Depense majeure"),
                transaction(LocalDate.now().minusMonths(1), 1500.0, TransactionType.DEPENSE, TransactionCategory.RESTAURANT, "Ancienne depense")
        ));
        when(budgetTargetService.getActiveBudgetTargetsForCurrentUser(user.getEmail())).thenReturn(List.of());

        String context = ragService.buildAdaptiveContext(1L, "Quel est mon solde net du mois ?");

        assertThat(context).contains("[MONTHLY_SOURCE_OF_TRUTH]");
        assertThat(context).contains("Revenus du mois courant: 22602.99 DT");
        assertThat(context).contains("Depenses du mois courant: 21000 DT");
        assertThat(context).contains("Solde net du mois courant: 1602.99 DT");
        assertThat(context).contains("Interdiction IA: ne jamais recalculer");
        assertThat(context).doesNotContain("99999");
    }

    @Test
    void buildAdaptiveContext_keepsOnlyCurrentMonthTransactionsInSupportSection() {
        User user = sampleUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(recommendationService.generateRecommendationsForUser(1L)).thenReturn(RecommendationResponseDto.builder()
                .summary(RecommendationSummaryDto.builder()
                        .currentMonthSeverity(CurrentMonthSeverity.NORMAL)
                        .currentMonthIncome(5000.0)
                        .currentMonthExpenses(2000.0)
                        .currentMonthNetBalance(3000.0)
                        .currentMonthSavingsRate(60.0)
                        .build())
                .recommendations(List.of())
                .build());
        when(transactionRepository.findAllByUserId(1L)).thenReturn(List.of(
                transaction(LocalDate.now().minusDays(1), 120.0, TransactionType.DEPENSE, TransactionCategory.RESTAURANT, "Restaurant du mois"),
                transaction(LocalDate.now().minusMonths(2), 400.0, TransactionType.DEPENSE, TransactionCategory.RESTAURANT, "Restaurant ancien")
        ));
        when(budgetTargetService.getActiveBudgetTargetsForCurrentUser(user.getEmail())).thenReturn(List.of());

        String context = ragService.buildAdaptiveContext(1L, "Quelles sont mes plus grosses depenses ?");

        assertThat(context).contains("Restaurant du mois");
        assertThat(context).doesNotContain("Restaurant ancien");
        assertThat(context).contains("Nombre de transactions du mois courant: 1");
    }

    private User sampleUser() {
        return User.builder()
                .id(1L)
                .email("user@attijari.test")
                .password("secret")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Transaction transaction(
            LocalDate date,
            double amount,
            TransactionType type,
            TransactionCategory category,
            String description
    ) {
        return Transaction.builder()
                .description(description)
                .merchantName(description)
                .amount(amount)
                .date(date)
                .type(type)
                .category(category)
                .user(sampleUser())
                .build();
    }
}

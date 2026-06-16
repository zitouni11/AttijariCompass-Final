package com.adem.attijari_compass.recommendation.service;

import com.adem.attijari_compass.dto.income.IncomeClassifiedTransaction;
import com.adem.attijari_compass.dto.income.IncomeClassificationResult;
import com.adem.attijari_compass.dto.income.IncomeInsightResponse;
import com.adem.attijari_compass.dto.income.IncomeRecommendation;
import com.adem.attijari_compass.dto.income.IncomeTransactionSnapshot;
import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.recommendation.dto.RecommendationDto;
import com.adem.attijari_compass.repository.TransactionRepository;
import com.adem.attijari_compass.repository.UserRepository;
import com.adem.attijari_compass.service.income.IncomeClassificationOrchestratorService;
import com.adem.attijari_compass.service.income.IncomeInsightService;
import com.adem.attijari_compass.service.income.IncomeRecommendationService;
import com.adem.attijari_compass.service.income.IncomeTypes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncomeRecommendationSourceServiceImpl implements IncomeRecommendationSourceService {

    private static final int MAX_INCOME_TRANSACTIONS = 12;
    private static final int MIN_CONFIDENCE_SCORE = 45;

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final IncomeClassificationOrchestratorService incomeClassificationOrchestratorService;
    private final IncomeInsightService incomeInsightService;
    private final IncomeRecommendationService incomeRecommendationService;
    private final IncomeRecommendationMapper incomeRecommendationMapper;

    @Override
    public List<RecommendationDto> generateRecommendationsForUser(Long userId) {
        List<IncomeTransactionSnapshot> incomeSnapshots = loadIncomeSnapshots(userId);
        if (incomeSnapshots.isEmpty()) {
            return List.of();
        }

        List<IncomeClassifiedTransaction> classifiedTransactions = classifyIncomeTransactions(incomeSnapshots);
        IncomeInsightResponse incomeInsight = incomeInsightService.analyze(classifiedTransactions);
        if (!shouldExposeRecommendations(incomeInsight)) {
            return List.of();
        }

        List<IncomeRecommendation> incomeRecommendations = incomeRecommendationService.generateRecommendations(incomeInsight);
        return incomeRecommendations.stream()
                .map(recommendation -> incomeRecommendationMapper.toRecommendationDto(recommendation, incomeInsight))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<RecommendationDto> generateRecommendationsForUser(String email) {
        Long userId = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email))
                .getId();
        return generateRecommendationsForUser(userId);
    }

    private List<IncomeTransactionSnapshot> loadIncomeSnapshots(Long userId) {
        return transactionRepository.findAllByUserIdAndType(userId, TransactionType.REVENU).stream()
                .filter(Objects::nonNull)
                .filter(transaction -> transaction.getDate() != null)
                .sorted(Comparator.comparing(Transaction::getDate).reversed())
                .limit(MAX_INCOME_TRANSACTIONS)
                .sorted(Comparator.comparing(Transaction::getDate))
                .map(this::toIncomeSnapshot)
                .toList();
    }

    private List<IncomeClassifiedTransaction> classifyIncomeTransactions(List<IncomeTransactionSnapshot> incomeSnapshots) {
        return incomeSnapshots.stream()
                .map(currentTransaction -> classifyIncomeTransaction(currentTransaction, incomeSnapshots))
                .toList();
    }

    private IncomeClassifiedTransaction classifyIncomeTransaction(IncomeTransactionSnapshot currentTransaction,
                                                                  List<IncomeTransactionSnapshot> historicalCredits) {
        IncomeClassificationResult classificationResult = incomeClassificationOrchestratorService.classifyIncome(
                currentTransaction,
                historicalCredits
        );

        IncomeClassifiedTransaction classifiedTransaction = new IncomeClassifiedTransaction();
        classifiedTransaction.setType(classificationResult != null ? classificationResult.getFinalType() : IncomeTypes.UNKNOWN);
        classifiedTransaction.setConfidence(classificationResult != null ? classificationResult.getFinalConfidence() : 0.0d);
        classifiedTransaction.setAmount(currentTransaction.getAmount());
        classifiedTransaction.setDate(currentTransaction.getTransactionDate());
        classifiedTransaction.setSource(classificationResult != null ? classificationResult.getSource() : null);
        return classifiedTransaction;
    }

    private boolean shouldExposeRecommendations(IncomeInsightResponse incomeInsight) {
        if (incomeInsight == null || incomeInsight.getIncomeConfidenceScore() == null) {
            return false;
        }

        String primaryIncomeType = IncomeTypes.normalize(incomeInsight.getPrimaryIncomeType());
        return incomeInsight.getIncomeConfidenceScore() >= MIN_CONFIDENCE_SCORE
                && (!IncomeTypes.UNKNOWN.equals(primaryIncomeType)
                || Boolean.TRUE.equals(incomeInsight.getHasSecondaryIncome()));
    }

    private IncomeTransactionSnapshot toIncomeSnapshot(Transaction transaction) {
        return new IncomeTransactionSnapshot(
                transaction.getMerchantName(),
                transaction.getDescription(),
                BigDecimal.valueOf(safeAmount(transaction.getAmount())),
                transaction.getDate()
        );
    }

    private double safeAmount(Double amount) {
        return amount != null ? Math.abs(amount) : 0.0d;
    }
}

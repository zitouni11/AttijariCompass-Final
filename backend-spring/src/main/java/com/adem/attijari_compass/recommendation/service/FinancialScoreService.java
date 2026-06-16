package com.adem.attijari_compass.recommendation.service;

import com.adem.attijari_compass.recommendation.dto.FinancialInsightDto;
import com.adem.attijari_compass.recommendation.dto.FinancialScoreBreakdownDto;
import com.adem.attijari_compass.recommendation.expense.ExpenseMetricsSnapshot;

public interface FinancialScoreService {

    FinancialScoreBreakdownDto calculate(
            Long userId,
            FinancialInsightDto insight,
            ExpenseMetricsSnapshot expenseSnapshot
    );
}

package com.adem.attijari_compass.service.income;

import com.adem.attijari_compass.dto.income.IncomeClassificationResult;
import com.adem.attijari_compass.dto.income.IncomeTransactionSnapshot;

import java.util.List;

public interface IncomeClassificationOrchestratorService {

    IncomeClassificationResult classifyIncome(IncomeTransactionSnapshot currentTransaction,
                                              List<IncomeTransactionSnapshot> historicalCredits);
}

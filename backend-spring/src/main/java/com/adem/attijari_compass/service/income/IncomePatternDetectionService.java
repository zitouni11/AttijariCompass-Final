package com.adem.attijari_compass.service.income;

import com.adem.attijari_compass.dto.income.IncomePatternDetectionResult;
import com.adem.attijari_compass.dto.income.IncomeTransactionSnapshot;

import java.util.List;

public interface IncomePatternDetectionService {

    IncomePatternDetectionResult detectPattern(IncomeTransactionSnapshot currentTransaction,
                                               List<IncomeTransactionSnapshot> historicalCredits);
}

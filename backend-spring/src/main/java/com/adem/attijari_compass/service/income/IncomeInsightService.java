package com.adem.attijari_compass.service.income;

import com.adem.attijari_compass.dto.income.IncomeClassifiedTransaction;
import com.adem.attijari_compass.dto.income.IncomeInsightResponse;

import java.util.List;

public interface IncomeInsightService {

    IncomeInsightResponse analyze(List<IncomeClassifiedTransaction> incomes);
}

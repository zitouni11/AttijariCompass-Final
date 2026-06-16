package com.adem.attijari_compass.service.income;

import com.adem.attijari_compass.dto.income.IncomeInsightResponse;
import com.adem.attijari_compass.dto.income.IncomeRecommendation;

import java.util.List;

public interface IncomeRecommendationService {

    List<IncomeRecommendation> generateRecommendations(IncomeInsightResponse insight);
}

package com.adem.attijari_compass.recommendation.service;

import com.adem.attijari_compass.recommendation.dto.FinancialInsightDto;

public interface FinancialAnalysisService {

    FinancialInsightDto analyzeUserFinancials(Long userId);

    FinancialInsightDto analyzeUserFinancials(String email);
}

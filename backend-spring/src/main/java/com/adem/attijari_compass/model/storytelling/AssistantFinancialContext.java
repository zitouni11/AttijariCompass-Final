package com.adem.attijari_compass.model.storytelling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantFinancialContext {
    private BigDecimal salary;
    private BigDecimal income;
    private BigDecimal expenses;
    private BigDecimal budget;
    private BigDecimal balance;
    private BigDecimal savingsBalance;
    private String currency;
    private boolean realDataAvailable;
    private boolean balanceAvailable;
    private boolean savingsBalanceAvailable;
    private List<String> recentTransactions;
    private Map<String, BigDecimal> spendingByCategory;
    private Map<String, Object> additionalData;
    private String monthlySummary;
}

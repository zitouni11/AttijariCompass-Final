package com.adem.attijari_compass.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardResponse {

    private String month;
    private BigDecimal income;
    private BigDecimal expenses;
    private BigDecimal netBalance;
    private BigDecimal savingsRate;
    private Integer trackedTransactions;
    private List<DashboardExpenseCategoryResponse> expenseByCategory;
    private DashboardFinancialHealthResponse financialHealth;

    // Legacy fields kept for frontend compatibility.
    private BigDecimal totalRevenu;
    private BigDecimal totalDepenses;
    private BigDecimal soldeActuel;
    private BigDecimal tauxEpargne;
    private BigDecimal resteAVivre;
    private Map<String, BigDecimal> depensesParCategorie;
    private Integer nombreTransactions;
    private String moisCourant;
}

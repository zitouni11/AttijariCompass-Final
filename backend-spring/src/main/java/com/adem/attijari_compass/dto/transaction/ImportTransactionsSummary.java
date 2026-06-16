package com.adem.attijari_compass.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportTransactionsSummary {

    private int categorizedCount;
    private int expenseCount;
    private int incomeCount;
    private double totalExpenses;
    private double totalIncome;
    private double netAmount;
}

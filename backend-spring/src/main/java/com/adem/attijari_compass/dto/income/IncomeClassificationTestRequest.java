package com.adem.attijari_compass.dto.income;

import java.util.List;

public class IncomeClassificationTestRequest {

    private IncomeTransactionSnapshot currentTransaction;
    private List<IncomeTransactionSnapshot> historicalCredits;

    public IncomeClassificationTestRequest() {
    }

    public IncomeClassificationTestRequest(IncomeTransactionSnapshot currentTransaction,
                                           List<IncomeTransactionSnapshot> historicalCredits) {
        this.currentTransaction = currentTransaction;
        this.historicalCredits = historicalCredits;
    }

    public IncomeTransactionSnapshot getCurrentTransaction() {
        return currentTransaction;
    }

    public void setCurrentTransaction(IncomeTransactionSnapshot currentTransaction) {
        this.currentTransaction = currentTransaction;
    }

    public List<IncomeTransactionSnapshot> getHistoricalCredits() {
        return historicalCredits;
    }

    public void setHistoricalCredits(List<IncomeTransactionSnapshot> historicalCredits) {
        this.historicalCredits = historicalCredits;
    }

    @Override
    public String toString() {
        return "IncomeClassificationTestRequest{" +
                "currentTransaction=" + currentTransaction +
                ", historicalCredits=" + historicalCredits +
                '}';
    }
}

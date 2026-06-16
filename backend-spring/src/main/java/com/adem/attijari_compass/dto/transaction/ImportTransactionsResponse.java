package com.adem.attijari_compass.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour la réponse d'import de transactions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportTransactionsResponse {

    private int totalProcessed;
    private int successCount;
    private int importedCount;
    private int errorCount;
    private List<ImportTransactionErrorRow> errors;
    private String message;
    private List<TransactionResponse> transactions;
    private ImportTransactionsSummary summary;
}


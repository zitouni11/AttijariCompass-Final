package com.adem.attijari_compass.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Réponse paginée pour les transactions
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaginatedTransactionResponse {
    private List<TransactionResponse> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;
    private boolean first;
}


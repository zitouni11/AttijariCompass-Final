package com.adem.attijari_compass.dto.transaction;

import com.adem.attijari_compass.entity.TransactionCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransactionCashBreakdownItemRequest(
        @NotNull TransactionCategory category,
        @NotNull @Positive Double amount,
        String note
) {
}

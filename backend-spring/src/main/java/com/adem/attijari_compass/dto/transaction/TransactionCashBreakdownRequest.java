package com.adem.attijari_compass.dto.transaction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TransactionCashBreakdownRequest(
        @NotEmpty List<@Valid TransactionCashBreakdownItemRequest> items
) {
}

package com.adem.attijari_compass.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCashBreakdownResponse {

    private Long transactionId;
    private Double transactionAmount;
    private Double allocatedAmount;
    private Double remainingAmount;
    private Boolean complete;
    private List<TransactionCashBreakdownItemResponse> items;
}

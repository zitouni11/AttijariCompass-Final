package com.adem.attijari_compass.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCashBreakdownItemResponse {

    private Long id;
    private String category;
    private String categoryLabel;
    private Double amount;
    private String note;
}

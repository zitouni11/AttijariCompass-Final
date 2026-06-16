package com.adem.attijari_compass.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportTransactionErrorRow {

    private int rowNumber;
    private String message;
}

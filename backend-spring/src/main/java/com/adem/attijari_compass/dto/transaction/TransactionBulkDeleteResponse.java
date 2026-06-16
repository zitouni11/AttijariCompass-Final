package com.adem.attijari_compass.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionBulkDeleteResponse {

    private String message;
    private Integer deletedCount;
    private LocalDateTime timestamp;
}

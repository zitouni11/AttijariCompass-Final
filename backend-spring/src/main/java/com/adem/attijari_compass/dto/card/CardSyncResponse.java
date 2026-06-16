package com.adem.attijari_compass.dto.card;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSyncResponse {
    private String message;
    private UserCardResponse card;
    private int importedTransactions;
    private int skippedTransactions;
    private LocalDateTime syncedAt;
}

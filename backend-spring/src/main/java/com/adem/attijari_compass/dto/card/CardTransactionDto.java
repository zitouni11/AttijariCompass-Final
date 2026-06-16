package com.adem.attijari_compass.dto.card;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardTransactionDto {
    private LocalDateTime date;
    private String merchantName;
    private String category;
    private BigDecimal amount;
}

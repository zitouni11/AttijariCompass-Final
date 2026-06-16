package com.adem.attijari_compass.dto.card;

import com.adem.attijari_compass.entity.SandboxTransactionType;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionSource;
import com.adem.attijari_compass.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardTransactionResponse {
    private Long id;
    private String merchantName;
    private String description;
    private Double amount;
    private SandboxTransactionType transactionType;
    private TransactionType type;
    private LocalDate date;
    private TransactionCategory category;
    private TransactionSource source;
    private String maskedCardNumber;
    private String cardLast4;
    private String externalReference;
    private LocalDateTime importedAt;
}

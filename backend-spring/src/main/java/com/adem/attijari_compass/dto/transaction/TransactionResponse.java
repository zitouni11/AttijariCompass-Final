package com.adem.attijari_compass.dto.transaction;

import com.adem.attijari_compass.entity.PaymentMethod;
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
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponse {
    private Long id;
    private String description;
    private Double amount;
    private LocalDate date;
    private TransactionCategory category;
    private TransactionType type;
    private Long userId;

    // Nouveaux champs pour fintech
    private String merchantName;
    private PaymentMethod paymentMethod;
    private TransactionSource source;
    private String cardLast4;
    private String maskedCardNumber;
    private Long userCardId;
    private String externalReference;
    private LocalDateTime createdAt;
    private Double categorizationConfidence;
    private String categorizationSource;
    private String categorizationNormalizedText;
}



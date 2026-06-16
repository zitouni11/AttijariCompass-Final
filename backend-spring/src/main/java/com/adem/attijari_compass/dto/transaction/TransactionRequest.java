package com.adem.attijari_compass.dto.transaction;

import com.adem.attijari_compass.entity.PaymentMethod;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionSource;
import com.adem.attijari_compass.entity.TransactionType;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TransactionRequest {

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    @NotNull(message = "Date is required")
    private LocalDate date;

    private TransactionCategory category;

    private TransactionCategory predictedCategory;

    @NotNull(message = "Type is required")
    private TransactionType type;

    private String merchantName;

    @JsonAlias("method")
    private PaymentMethod paymentMethod = PaymentMethod.CARD;

    private Long userCardId;

    private TransactionSource source;

    private String categorizationSource;

    private Double categorizationConfidence;

    private String normalizedText;
}

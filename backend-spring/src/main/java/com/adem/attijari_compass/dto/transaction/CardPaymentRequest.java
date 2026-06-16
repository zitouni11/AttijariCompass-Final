package com.adem.attijari_compass.dto.transaction;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO pour enregistrer un paiement par carte
 * La catégorisation se fait automatiquement côté serveur
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CardPaymentRequest {

    @NotBlank(message = "Merchant name is required")
    @Size(max = 100, message = "Merchant name must be <= 100 characters")
    private String merchantName;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be >= 0.01")
    private Double amount;

    @NotNull(message = "Transaction date is required")
    private LocalDate date;

    @Size(max = 255, message = "Description must be <= 255 characters")
    private String description;

    @NotBlank(message = "Card last 4 digits are required")
    @Pattern(regexp = "\\d{4}", message = "Card last 4 must be exactly 4 digits")
    private String cardLast4;

    // Optional : code MCC si disponible depuis API bancaire
    @Pattern(regexp = "\\d{4}", message = "MCC must be 4 digits")
    private String mcc;
}


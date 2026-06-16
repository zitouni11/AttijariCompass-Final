package com.adem.attijari_compass.dto.card;

import com.adem.attijari_compass.entity.CardScope;
import com.adem.attijari_compass.entity.CardSourceType;
import com.adem.attijari_compass.entity.CardStatus;
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
public class CardDetailsDto {
    private Long id;
    private Long cardCatalogId;
    private String cardCatalogCode;
    private String cardCatalogName;
    private String description;
    private String brand;
    private CardScope scope;
    private String cardHolderName;
    private String maskedCardNumber;
    private String last4;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String cardCode;
    private CardStatus cardStatus;
    private boolean primaryCard;
    private boolean active;
    private CardSourceType sourceType;
    private BigDecimal maxPaymentLimit;
    private BigDecimal maxWithdrawalLimit;
    private boolean allowsOnlinePayment;
    private boolean allowsInternationalPayment;
    private boolean allowsInstallments;
    private Integer installmentMonthsMax;
    private String imageUrl;
    private LocalDateTime linkedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

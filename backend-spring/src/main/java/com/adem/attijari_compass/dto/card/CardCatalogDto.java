package com.adem.attijari_compass.dto.card;

import com.adem.attijari_compass.entity.CardScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardCatalogDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String brand;
    private CardScope scope;
    private BigDecimal maxPaymentLimit;
    private BigDecimal maxWithdrawalLimit;
    private boolean allowsOnlinePayment;
    private boolean allowsInternationalPayment;
    private boolean allowsInstallments;
    private Integer installmentMonthsMax;
    private String imageUrl;
    private boolean active;
}

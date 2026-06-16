package com.adem.attijari_compass.dto.card;

import com.adem.attijari_compass.entity.CardStatus;
import com.adem.attijari_compass.entity.CardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedTestCardResponse {
    private Long id;
    private String holderName;
    private String maskedCardNumber;
    private String testCardNumber;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String cvv;
    private CardType cardType;
    private String bankName;
    private CardStatus status;
    private Double initialBalance;
}

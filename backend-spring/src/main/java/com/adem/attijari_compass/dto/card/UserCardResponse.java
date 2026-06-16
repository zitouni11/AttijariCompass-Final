package com.adem.attijari_compass.dto.card;

import com.adem.attijari_compass.entity.CardStatus;
import com.adem.attijari_compass.entity.CardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCardResponse {
    private Long id;
    private Long linkedTestCardId;
    private String holderName;
    private String maskedCardNumber;
    private CardType cardType;
    private String bankName;
    private CardStatus status;
    private LocalDateTime connectedAt;
    private LocalDateTime lastSyncAt;
    private boolean active;
}

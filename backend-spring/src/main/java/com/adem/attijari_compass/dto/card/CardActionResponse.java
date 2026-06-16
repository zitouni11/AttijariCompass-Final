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
public class CardActionResponse {
    private String message;
    private Long cardId;
    private boolean active;
    private LocalDateTime timestamp;
}

package com.adem.attijari_compass.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDto {
    private String answer;
    private String ragContextPreview;
    private String usedModel;
    private OffsetDateTime timestamp;
}

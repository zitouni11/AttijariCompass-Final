package com.adem.attijari_compass.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroqResponseDto {
    private List<ChoiceDto> choices;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChoiceDto {
        private MessageDto message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageDto {
        private String role;
        private String content;
    }
}

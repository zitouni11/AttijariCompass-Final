package com.adem.attijari_compass.dto.card;

import com.adem.attijari_compass.entity.SandboxCardProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateTestCardResponse {
    private String message;
    private SandboxCardProfile profile;
    private GeneratedTestCardResponse card;
    private int generatedTransactions;
    private int importedTransactions;
    private boolean connectedToCurrentUser;
    private UserCardResponse connectedCard;
    private LocalDateTime syncedAt;
}

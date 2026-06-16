package com.adem.attijari_compass.dto.card;

import com.adem.attijari_compass.entity.SandboxCardProfile;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateTestCardRequest {

    @NotBlank(message = "Holder name is required")
    @Size(max = 100, message = "Holder name must be <= 100 characters")
    private String holderName;

    @NotNull(message = "Profile is required")
    private SandboxCardProfile profile;

    @NotNull(message = "Transaction count is required")
    @Min(value = 1, message = "Transaction count must be at least 1")
    @Max(value = 100, message = "Transaction count must be <= 100")
    private Integer transactionCount;

    @Builder.Default
    private boolean connectToCurrentUser = false;
}

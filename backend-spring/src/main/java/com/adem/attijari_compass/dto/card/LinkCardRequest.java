package com.adem.attijari_compass.dto.card;

import com.adem.attijari_compass.util.CardMaskingUtil;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "cardNumber")
public class LinkCardRequest {

    @NotNull
    private Long cardCatalogId;

    @Size(max = 150)
    private String cardHolderName;

    @NotBlank
    @Size(max = 25)
    @Pattern(regexp = "^[0-9\\s-]+$", message = "cardNumber must contain only digits, spaces or hyphens")
    private String cardNumber;

    @NotNull
    @Min(1)
    @Max(12)
    private Integer expiryMonth;

    @NotNull
    @Min(2000)
    @Max(9999)
    private Integer expiryYear;

    @AssertTrue(message = "cardNumber must contain between 13 and 19 digits")
    public boolean isCardNumberConsistent() {
        if (cardNumber == null) {
            return true;
        }

        String normalized = CardMaskingUtil.normalizeCardNumber(cardNumber);
        return normalized.length() >= 13 && normalized.length() <= 19;
    }
}

package com.adem.attijari_compass.dto.card;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectTestCardRequest {

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "[\\d\\s-]{13,25}", message = "Card number must contain 13 to 19 digits")
    private String cardNumber;

    @NotBlank(message = "Holder name is required")
    @Size(max = 100, message = "Holder name must be <= 100 characters")
    private String holderName;

    @NotNull(message = "Expiry month is required")
    @Min(value = 1, message = "Expiry month must be between 1 and 12")
    @Max(value = 12, message = "Expiry month must be between 1 and 12")
    private Integer expiryMonth;

    @NotNull(message = "Expiry year is required")
    @Min(value = 2024, message = "Expiry year is invalid")
    @Max(value = 2100, message = "Expiry year is invalid")
    private Integer expiryYear;

    @NotBlank(message = "CVV is required")
    @Pattern(regexp = "\\d{3,4}", message = "CVV must contain 3 or 4 digits")
    private String cvv;
}

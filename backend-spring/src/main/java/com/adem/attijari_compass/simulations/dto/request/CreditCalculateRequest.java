package com.adem.attijari_compass.simulations.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditCalculateRequest {

    @NotNull
    @Positive
    @Schema(example = "220000")
    private BigDecimal loanAmount;

    @PositiveOrZero
    @Schema(example = "30000")
    private BigDecimal downPayment;

    @NotNull
    @PositiveOrZero
    @Schema(example = "7.1")
    private BigDecimal annualInterestRate;

    @NotNull
    @Min(1)
    @Schema(example = "84")
    private Integer durationMonths;

    @NotNull
    @Positive
    @Schema(example = "6200")
    private BigDecimal monthlyIncome;

    @NotNull
    @PositiveOrZero
    @Schema(example = "800")
    private BigDecimal existingMonthlyCharges;

    @PositiveOrZero
    @Schema(example = "15000")
    private BigDecimal earlyRepaymentAmount;

    @Positive
    @Schema(example = "24")
    private Integer earlyRepaymentMonth;
}

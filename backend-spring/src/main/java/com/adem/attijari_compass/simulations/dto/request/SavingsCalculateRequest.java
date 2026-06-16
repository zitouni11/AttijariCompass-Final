package com.adem.attijari_compass.simulations.dto.request;

import com.adem.attijari_compass.simulations.model.ContributionFrequency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsCalculateRequest {

    @NotNull
    @Positive
    @Schema(example = "25000")
    private BigDecimal targetAmount;

    @PositiveOrZero
    @Schema(example = "4000")
    private BigDecimal initialAmount;

    @NotNull
    @Positive
    @Schema(example = "900")
    private BigDecimal monthlyContribution;

    @PositiveOrZero
    @Schema(example = "1500")
    private BigDecimal extraContribution;

    @NotNull
    @Schema(example = "MONTHLY", allowableValues = {"MONTHLY", "QUARTERLY"})
    private ContributionFrequency contributionFrequency;

    @Schema(example = "2026-04-03")
    private LocalDate startDate;

    @Schema(example = "2028-12-31")
    private LocalDate targetDate;
}

package com.adem.attijari_compass.simulations.dto.response;

import com.adem.attijari_compass.simulations.model.ContributionFrequency;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SavingsScenarioInputResponse {
    private BigDecimal targetAmount;
    private BigDecimal initialAmount;
    private BigDecimal monthlyContribution;
    private BigDecimal extraContribution;
    private ContributionFrequency contributionFrequency;
    private LocalDate startDate;
    private LocalDate targetDate;
}

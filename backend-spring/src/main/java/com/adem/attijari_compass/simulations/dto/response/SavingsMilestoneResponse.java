package com.adem.attijari_compass.simulations.dto.response;

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
public class SavingsMilestoneResponse {
    private String label;
    private Integer percentage;
    private Integer elapsedMonths;
    private LocalDate milestoneDate;
    private BigDecimal projectedAmount;
    private BigDecimal remainingAmount;
}

package com.adem.attijari_compass.simulations.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SavingsCalculateResponse {
    private Integer estimatedMonths;
    private LocalDate estimatedEndDate;
    private BigDecimal totalContributed;
    private BigDecimal remainingAmount;
    private List<SavingsMilestoneResponse> milestones;
    private List<SavingsProjectionPointResponse> projectionPoints;
    private String simulationSummary;
}

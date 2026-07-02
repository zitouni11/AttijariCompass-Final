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
public class CreditCalculateResponse {
    private BigDecimal financedAmount;
    private BigDecimal monthlyPayment;
    private BigDecimal totalCost;
    private BigDecimal totalInterest;
    private LocalDate endDate;
    private List<AmortizationPreviewItemResponse> amortizationPreview;
    private EarlyRepaymentImpactResponse earlyRepaymentImpact;
    private CreditEligibilityResponse eligibility;
}

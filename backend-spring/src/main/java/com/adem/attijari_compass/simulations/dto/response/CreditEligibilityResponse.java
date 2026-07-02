package com.adem.attijari_compass.simulations.dto.response;

import com.adem.attijari_compass.simulations.model.CreditEligibilityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditEligibilityResponse {
    private CreditEligibilityStatus status;
    private BigDecimal realRepaymentCapacity;
    private BigDecimal debtRatio;
    private BigDecimal maximumRecommendedAmount;
    private String message;
    private boolean recommended;
    private Integer recommendedDurationMonths;
    private BigDecimal recommendedChargeReduction;
}

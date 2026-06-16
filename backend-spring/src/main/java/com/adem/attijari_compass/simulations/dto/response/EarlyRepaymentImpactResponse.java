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
public class EarlyRepaymentImpactResponse {
    private BigDecimal appliedAmount;
    private Integer appliedMonth;
    private LocalDate originalEndDate;
    private LocalDate updatedEndDate;
    private Integer monthsSaved;
    private BigDecimal originalTotalInterest;
    private BigDecimal updatedTotalInterest;
    private BigDecimal interestSaved;
}

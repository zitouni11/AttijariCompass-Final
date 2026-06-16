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
public class AmortizationPreviewItemResponse {
    private Integer monthNumber;
    private LocalDate paymentDate;
    private BigDecimal openingBalance;
    private BigDecimal principalPaid;
    private BigDecimal interestPaid;
    private BigDecimal totalPayment;
    private BigDecimal closingBalance;
}

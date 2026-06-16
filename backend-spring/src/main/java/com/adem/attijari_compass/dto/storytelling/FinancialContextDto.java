package com.adem.attijari_compass.dto.storytelling;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinancialContextDto {

    private BigDecimal salary;

    private BigDecimal income;

    private BigDecimal expenses;

    private BigDecimal budget;

    private BigDecimal balance;

    private BigDecimal savingsBalance;

    private String currency;

    private Map<String, Object> additionalData;
}

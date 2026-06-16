package com.adem.attijari_compass.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardFinancialHealthResponse {

    private Integer score;
    private boolean positiveBalance;
    private List<String> insights;
}

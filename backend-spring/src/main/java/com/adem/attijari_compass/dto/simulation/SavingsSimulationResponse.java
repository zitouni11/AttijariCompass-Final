package com.adem.attijari_compass.dto.simulation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SavingsSimulationResponse {
    private Double montantEpargne;
    private Double objectifMontant;
    private Integer nombreMois;
    private Double totalEpargne;
    private String message;
}


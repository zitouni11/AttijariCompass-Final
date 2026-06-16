package com.adem.attijari_compass.dto.simulation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreditSimulationResponse {
    private Double montantCredit;
    private Double mensualite;
    private Double coutTotal;
    private Double tauxEndettement;
    private Double resteAVivre;
    private String scoreRisque;
    private String message;
}


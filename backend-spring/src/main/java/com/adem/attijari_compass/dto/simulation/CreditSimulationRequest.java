package com.adem.attijari_compass.dto.simulation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreditSimulationRequest {

    @NotNull
    @Positive
    private Double montantCredit;

    @NotNull
    @Positive
    private Double tauxInteret;

    @NotNull
    @Positive
    private Integer dureeEnMois;

    @NotNull
    @Positive
    private Double revenuMensuel;
}


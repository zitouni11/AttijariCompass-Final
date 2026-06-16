package com.adem.attijari_compass.dto.simulation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class SavingsSimulationRequest {

    @NotNull
    @Positive
    private Double montantEpargne;

    @NotNull
    @Positive
    private Double objectifMontant;

    private Double revenuMensuel;
}


package com.adem.attijari_compass.simulations.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsCompareRequest {

    @Valid
    @NotEmpty
    @Size(max = 3)
    private List<SavingsScenarioRequest> scenarios;
}

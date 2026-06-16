package com.adem.attijari_compass.dto.storytelling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyStoryResponse {
    private String mois;
    private String resume;
    private String categoriesPrincipales;
    private Double totalDepenses;
    private Double totalRevenus;
    private Double epargneRealisee;
    private List<String> alertes;
    private List<String> missions;
}


package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.simulation.CreditSimulationRequest;
import com.adem.attijari_compass.dto.simulation.CreditSimulationResponse;
import com.adem.attijari_compass.dto.simulation.SavingsSimulationRequest;
import com.adem.attijari_compass.dto.simulation.SavingsSimulationResponse;
import com.adem.attijari_compass.simulations.dto.request.CreditCalculateRequest;
import com.adem.attijari_compass.simulations.dto.request.SavingsCalculateRequest;
import com.adem.attijari_compass.simulations.dto.response.CreditCalculateResponse;
import com.adem.attijari_compass.simulations.dto.response.SavingsCalculateResponse;
import com.adem.attijari_compass.simulations.model.ContributionFrequency;
import com.adem.attijari_compass.simulations.service.CreditSimulationService;
import com.adem.attijari_compass.simulations.service.SavingsSimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private final SavingsSimulationService savingsSimulationService;
    private final CreditSimulationService creditSimulationService;

    public SavingsSimulationResponse simulateSavings(SavingsSimulationRequest request) {
        SavingsCalculateResponse response = savingsSimulationService.calculate(
                SavingsCalculateRequest.builder()
                        .targetAmount(BigDecimal.valueOf(request.getObjectifMontant()))
                        .initialAmount(BigDecimal.ZERO)
                        .monthlyContribution(BigDecimal.valueOf(request.getMontantEpargne()))
                        .extraContribution(BigDecimal.ZERO)
                        .contributionFrequency(ContributionFrequency.MONTHLY)
                        .build()
        );

        return SavingsSimulationResponse.builder()
                .montantEpargne(request.getMontantEpargne())
                .objectifMontant(request.getObjectifMontant())
                .nombreMois(response.getEstimatedMonths())
                .totalEpargne(response.getTotalContributed().doubleValue())
                .message(response.getSimulationSummary())
                .build();
    }

    public CreditSimulationResponse simulateCredit(CreditSimulationRequest request) {
        CreditCalculateResponse response = creditSimulationService.calculate(
                CreditCalculateRequest.builder()
                        .loanAmount(BigDecimal.valueOf(request.getMontantCredit()))
                        .downPayment(BigDecimal.ZERO)
                        .annualInterestRate(BigDecimal.valueOf(request.getTauxInteret()))
                        .durationMonths(request.getDureeEnMois())
                        .monthlyIncome(BigDecimal.valueOf(request.getRevenuMensuel()))
                        .build()
        );

        double mensualite = response.getMonthlyPayment().doubleValue();
        double revenuMensuel = request.getRevenuMensuel();
        double tauxEndettement = revenuMensuel <= 0 ? 0 : Math.round((mensualite / revenuMensuel) * 10_000.0) / 100.0;
        double resteAVivre = Math.round((revenuMensuel - mensualite) * 100.0) / 100.0;

        return CreditSimulationResponse.builder()
                .montantCredit(request.getMontantCredit())
                .mensualite(mensualite)
                .coutTotal(response.getTotalCost().doubleValue())
                .tauxEndettement(tauxEndettement)
                .resteAVivre(resteAVivre)
                .scoreRisque(resolveRiskScore(tauxEndettement))
                .message(buildLegacyCreditMessage(request, mensualite, tauxEndettement, resteAVivre))
                .build();
    }

    private String resolveRiskScore(double tauxEndettement) {
        if (tauxEndettement <= 20) {
            return "FAIBLE";
        }
        if (tauxEndettement <= 33) {
            return "MODERE";
        }
        if (tauxEndettement <= 40) {
            return "ELEVE";
        }
        return "TRES ELEVE - NON RECOMMANDE";
    }

    private String buildLegacyCreditMessage(
            CreditSimulationRequest request,
            double mensualite,
            double tauxEndettement,
            double resteAVivre) {
        return String.format(
                "Pour un credit de %.2f DT sur %d mois a %.2f%%, la mensualite estimee est de %.2f DT. Taux d'endettement: %.2f%%. Reste a vivre: %.2f DT.",
                request.getMontantCredit(),
                request.getDureeEnMois(),
                request.getTauxInteret(),
                mensualite,
                tauxEndettement,
                resteAVivre
        );
    }
}

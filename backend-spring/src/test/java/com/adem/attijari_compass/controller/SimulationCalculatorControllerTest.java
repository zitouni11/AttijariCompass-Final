package com.adem.attijari_compass.controller;

import com.adem.attijari_compass.security.JwtAuthenticationFilter;
import com.adem.attijari_compass.security.JwtService;
import com.adem.attijari_compass.simulations.controller.SimulationCalculatorController;
import com.adem.attijari_compass.simulations.dto.request.CreditCalculateRequest;
import com.adem.attijari_compass.simulations.dto.request.SavingsCalculateRequest;
import com.adem.attijari_compass.simulations.dto.response.CreditCalculateResponse;
import com.adem.attijari_compass.simulations.dto.response.SavingsCalculateResponse;
import com.adem.attijari_compass.simulations.model.ContributionFrequency;
import com.adem.attijari_compass.simulations.service.CreditSimulationService;
import com.adem.attijari_compass.simulations.service.SavingsSimulationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SimulationCalculatorController.class)
@AutoConfigureMockMvc(addFilters = false)
class SimulationCalculatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SavingsSimulationService savingsSimulationService;

    @MockBean
    private CreditSimulationService creditSimulationService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private AuthenticationProvider authenticationProvider;

    @Test
    void shouldCalculateSavingsScenario() throws Exception {
        when(savingsSimulationService.calculate(any(SavingsCalculateRequest.class)))
                .thenReturn(SavingsCalculateResponse.builder()
                        .estimatedMonths(18)
                        .estimatedEndDate(LocalDate.of(2027, 10, 1))
                        .totalContributed(BigDecimal.valueOf(20_500))
                        .remainingAmount(BigDecimal.valueOf(17_000))
                        .milestones(List.of())
                        .projectionPoints(List.of())
                        .simulationSummary("summary")
                        .build());

        mockMvc.perform(post("/api/simulations/savings/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SavingsCalculateRequest.builder()
                                .targetAmount(BigDecimal.valueOf(25_000))
                                .initialAmount(BigDecimal.valueOf(4_000))
                                .monthlyContribution(BigDecimal.valueOf(900))
                                .extraContribution(BigDecimal.valueOf(1_500))
                                .contributionFrequency(ContributionFrequency.MONTHLY)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedMonths").value(18))
                .andExpect(jsonPath("$.simulationSummary").value("summary"));
    }

    @Test
    void shouldCalculateCreditScenario() throws Exception {
        when(creditSimulationService.calculate(any(CreditCalculateRequest.class)))
                .thenReturn(CreditCalculateResponse.builder()
                        .financedAmount(BigDecimal.valueOf(190_000))
                        .monthlyPayment(BigDecimal.valueOf(2_850.75))
                        .totalCost(BigDecimal.valueOf(269_461.20))
                        .totalInterest(BigDecimal.valueOf(49_461.20))
                        .endDate(LocalDate.of(2033, 4, 3))
                        .amortizationPreview(List.of())
                        .build());

        mockMvc.perform(post("/api/simulations/credit/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreditCalculateRequest.builder()
                                .loanAmount(BigDecimal.valueOf(220_000))
                                .downPayment(BigDecimal.valueOf(30_000))
                                .annualInterestRate(BigDecimal.valueOf(7.1))
                                .durationMonths(84)
                                .monthlyIncome(BigDecimal.valueOf(6_200))
                                .existingMonthlyCharges(BigDecimal.ZERO)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.financedAmount").value(190000))
                .andExpect(jsonPath("$.monthlyPayment").value(2850.75));
    }

    @Test
    void shouldRejectInvalidSavingsComparePayload() throws Exception {
        mockMvc.perform(post("/api/simulations/savings/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenarios\":[]}"))
                .andExpect(status().isBadRequest());
    }
}

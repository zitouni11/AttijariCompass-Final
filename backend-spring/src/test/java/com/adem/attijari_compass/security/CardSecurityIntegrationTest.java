package com.adem.attijari_compass.security;

import com.adem.attijari_compass.dto.card.CardTransactionResponse;
import com.adem.attijari_compass.dto.card.CardSyncResponse;
import com.adem.attijari_compass.dto.card.GenerateTestCardResponse;
import com.adem.attijari_compass.dto.card.GeneratedTestCardResponse;
import com.adem.attijari_compass.dto.card.UserCardResponse;
import com.adem.attijari_compass.entity.CardStatus;
import com.adem.attijari_compass.entity.CardType;
import com.adem.attijari_compass.entity.SandboxCardProfile;
import com.adem.attijari_compass.entity.SandboxTransactionType;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionSource;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.exception.AuthenticationRequiredException;
import com.adem.attijari_compass.service.CardService;
import com.adem.attijari_compass.service.SandboxCardGenerationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CardSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardService cardService;

    @MockBean
    private SandboxCardGenerationService sandboxCardGenerationService;

    @Test
    void shouldExposeSwaggerDocsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnUnauthorizedForSandboxCardConnectionWhenNoJwtIsProvided() throws Exception {
        mockMvc.perform(post("/api/cards/test/connect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConnectRequestPayload())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void shouldReturnUnauthorizedForSandboxCardConnectionByIdWhenNoJwtIsProvided() throws Exception {
        mockMvc.perform(post("/api/cards/test/10/connect"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void shouldAllowSandboxCardGenerationWithoutAuthenticationWhenAutoConnectIsFalse() throws Exception {
        when(sandboxCardGenerationService.generateSandboxCard(any(), eq(null)))
                .thenReturn(GenerateTestCardResponse.builder()
                        .message("Sandbox card generated successfully")
                        .profile(SandboxCardProfile.STUDENT)
                        .card(GeneratedTestCardResponse.builder()
                                .id(50L)
                                .holderName("ZITA TEST")
                                .maskedCardNumber("**** **** **** 4242")
                                .testCardNumber("4242424242424242")
                                .expiryMonth(12)
                                .expiryYear(2029)
                                .cvv("123")
                                .cardType(CardType.VISA)
                                .bankName("Attijari Bank Tunisie")
                                .status(CardStatus.ACTIVE)
                                .initialBalance(900.0)
                                .build())
                        .generatedTransactions(30)
                        .importedTransactions(0)
                        .connectedToCurrentUser(false)
                        .build());

        mockMvc.perform(post("/api/cards/test/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenerateRequestPayload("STUDENT", 30, false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Sandbox card generated successfully"))
                .andExpect(jsonPath("$.profile").value("STUDENT"))
                .andExpect(jsonPath("$.card.testCardNumber").value("4242424242424242"));
    }

    @Test
    void shouldReturnBusinessUnauthorizedWhenAnonymousAutoConnectionIsRequested() throws Exception {
        when(sandboxCardGenerationService.generateSandboxCard(any(), eq(null)))
                .thenThrow(new AuthenticationRequiredException(
                        "Authentication is required to connect the generated card to the current user"
                ));

        mockMvc.perform(post("/api/cards/test/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenerateRequestPayload("STUDENT", 30, true))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void shouldReturnBadRequestForInvalidProfileValue() throws Exception {
        mockMvc.perform(post("/api/cards/test/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenerateRequestPayload("Gold - Unknown", 30, false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid profile value"));

        verify(sandboxCardGenerationService, never()).generateSandboxCard(any(), any());
    }

    @Test
    void shouldAcceptFrontendProfileLabelWhenItMatchesKnownEnumPrefix() throws Exception {
        when(sandboxCardGenerationService.generateSandboxCard(any(), eq(null)))
                .thenReturn(GenerateTestCardResponse.builder()
                        .message("Sandbox card generated successfully")
                        .profile(SandboxCardProfile.PREMIUM)
                        .card(GeneratedTestCardResponse.builder()
                                .id(77L)
                                .holderName("ZITA TEST")
                                .maskedCardNumber("**** **** **** 1111")
                                .testCardNumber("4111111111111111")
                                .expiryMonth(10)
                                .expiryYear(2029)
                                .cvv("987")
                                .cardType(CardType.VISA)
                                .bankName("Attijari Bank Tunisie")
                                .status(CardStatus.ACTIVE)
                                .initialBalance(50000.0)
                                .build())
                        .generatedTransactions(20)
                        .importedTransactions(0)
                        .connectedToCurrentUser(false)
                        .build());

        mockMvc.perform(post("/api/cards/test/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenerateRequestPayload("Premium - Volume eleve", 20, false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.profile").value("PREMIUM"));
    }

    @Test
    void shouldValidateTransactionCountRange() throws Exception {
        mockMvc.perform(post("/api/cards/test/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenerateRequestPayload("STUDENT", 0, false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.transactionCount").value("Transaction count must be at least 1"));

        verify(sandboxCardGenerationService, never()).generateSandboxCard(any(), any());
    }

    @Test
    void shouldRejectTransactionCountAboveConfiguredMaximum() throws Exception {
        mockMvc.perform(post("/api/cards/test/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenerateRequestPayload("STUDENT", 101, false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.transactionCount").value("Transaction count must be <= 100"));

        verify(sandboxCardGenerationService, never()).generateSandboxCard(any(), any());
    }

    @Test
    @WithMockUser(username = "user@attijari.tn", roles = {"USER"})
    void shouldAllowSandboxCardConnectionForAuthenticatedUser() throws Exception {
        when(cardService.connectTestCard(any(), eq("user@attijari.tn")))
                .thenReturn(CardSyncResponse.builder()
                        .message("Sandbox card connected successfully")
                        .card(UserCardResponse.builder()
                                .id(1L)
                                .linkedTestCardId(10L)
                                .holderName("Test User")
                                .maskedCardNumber("**** **** **** 4242")
                                .cardType(CardType.VISA)
                                .bankName("Attijari Bank Tunisie")
                                .status(CardStatus.ACTIVE)
                                .connectedAt(LocalDateTime.now())
                                .active(true)
                                .build())
                        .importedTransactions(12)
                        .skippedTransactions(0)
                        .syncedAt(LocalDateTime.now())
                        .build());

        mockMvc.perform(post("/api/cards/test/connect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConnectRequestPayload())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Sandbox card connected successfully"))
                .andExpect(jsonPath("$.card.maskedCardNumber").value("**** **** **** 4242"));

        verify(cardService).connectTestCard(any(), eq("user@attijari.tn"));
    }

    @Test
    @WithMockUser(username = "user@attijari.tn", roles = {"USER"})
    void shouldAllowSandboxCardConnectionByIdForAuthenticatedUser() throws Exception {
        when(cardService.connectTestCardById(10L, "user@attijari.tn"))
                .thenReturn(CardSyncResponse.builder()
                        .message("Sandbox card connected successfully")
                        .card(UserCardResponse.builder()
                                .id(2L)
                                .linkedTestCardId(10L)
                                .holderName("Generated User")
                                .maskedCardNumber("**** **** **** 1111")
                                .cardType(CardType.MASTERCARD)
                                .bankName("Attijari Bank Tunisie")
                                .status(CardStatus.ACTIVE)
                                .connectedAt(LocalDateTime.now())
                                .active(true)
                                .build())
                        .importedTransactions(18)
                        .skippedTransactions(0)
                        .syncedAt(LocalDateTime.now())
                        .build());

        mockMvc.perform(post("/api/cards/test/10/connect"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Sandbox card connected successfully"))
                .andExpect(jsonPath("$.card.linkedTestCardId").value(10))
                .andExpect(jsonPath("$.importedTransactions").value(18));

        verify(cardService).connectTestCardById(10L, "user@attijari.tn");
    }

    @Test
    @WithMockUser(username = "user@attijari.tn", roles = {"USER"})
    void shouldExposeTransactionTypeOnCardTransactionsEndpoint() throws Exception {
        when(cardService.getCardTransactions(44L, "user@attijari.tn"))
                .thenReturn(List.of(
                        CardTransactionResponse.builder()
                                .id(901L)
                                .merchantName("Hotel")
                                .description("Sejour hotel")
                                .amount(680.0)
                                .transactionType(SandboxTransactionType.DEBIT)
                                .type(TransactionType.DEPENSE)
                                .date(LocalDate.of(2026, 4, 1))
                                .category(TransactionCategory.DIVERTISSEMENT)
                                .source(TransactionSource.CARD_SANDBOX)
                                .maskedCardNumber("**** **** **** 1111")
                                .cardLast4("1111")
                                .externalReference("PREM-HTL-001")
                                .importedAt(LocalDateTime.of(2026, 4, 1, 9, 30))
                                .build()
                ));

        mockMvc.perform(get("/api/cards/44/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionType").value("DEBIT"))
                .andExpect(jsonPath("$[0].type").value("DEPENSE"))
                .andExpect(jsonPath("$[0].source").value("CARD_SANDBOX"));
    }

    @Test
    @WithMockUser(username = "user@attijari.tn", roles = {"USER"})
    void shouldKeepAdminOnlyUserListingProtected() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    private static class ConnectRequestPayload {
        public String cardNumber = "4242424242424242";
        public String holderName = "Test User";
        public Integer expiryMonth = 12;
        public Integer expiryYear = 2028;
        public String cvv = "123";
    }

    private static class GenerateRequestPayload {
        public String holderName = "Zita Test";
        public String profile;
        public Integer transactionCount;
        public boolean connectToCurrentUser;

        private GenerateRequestPayload(String profile, Integer transactionCount, boolean connectToCurrentUser) {
            this.profile = profile;
            this.transactionCount = transactionCount;
            this.connectToCurrentUser = connectToCurrentUser;
        }
    }
}

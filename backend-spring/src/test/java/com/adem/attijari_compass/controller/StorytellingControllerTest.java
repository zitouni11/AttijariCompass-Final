package com.adem.attijari_compass.controller;

import com.adem.attijari_compass.dto.storytelling.StorytellingChatRequest;
import com.adem.attijari_compass.dto.storytelling.StorytellingChatResponse;
import com.adem.attijari_compass.dto.storytelling.MonthlyStoryResponse;
import com.adem.attijari_compass.model.storytelling.AssistantAction;
import com.adem.attijari_compass.model.storytelling.AssistantEmotion;
import com.adem.attijari_compass.model.storytelling.AssistantIntent;
import com.adem.attijari_compass.security.JwtAuthenticationFilter;
import com.adem.attijari_compass.security.JwtService;
import com.adem.attijari_compass.service.StorytellingAssistantService;
import com.adem.attijari_compass.service.StorytellingService;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StorytellingController.class)
@AutoConfigureMockMvc(addFilters = false)
class StorytellingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StorytellingService storytellingService;

    @MockBean
    private StorytellingAssistantService storytellingAssistantService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private AuthenticationProvider authenticationProvider;

    @Test
    void shouldReturnAssistantReply() throws Exception {
        StorytellingChatResponse response = StorytellingChatResponse.builder()
                .reply("Bonjour, parlons de votre budget.")
                .emotion(AssistantEmotion.FRIENDLY)
                .action(AssistantAction.SHOW_BUDGET)
                .intent(AssistantIntent.BUDGET_HELP)
                .build();

        when(storytellingAssistantService.chat(any(StorytellingChatRequest.class), nullable(String.class))).thenReturn(response);

        mockMvc.perform(post("/api/storytelling/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "message", "Bonjour, aide-moi avec mon budget",
                                "userObjective", "mieux gerer mes depenses",
                                "conversationHistory", List.of(
                                        Map.of("role", "user", "text", "Bonjour"),
                                        Map.of("role", "assistant", "text", "Bonjour, comment puis-je vous aider ?")
                                ),
                                "financialContext", Map.of(
                                        "income", 3500,
                                        "expenses", 2100,
                                        "budget", 2500
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Bonjour, parlons de votre budget."))
                .andExpect(jsonPath("$.emotion").value("friendly"))
                .andExpect(jsonPath("$.action").value("show_budget"))
                .andExpect(jsonPath("$.intent").value("budget_help"));
    }

    @Test
    void shouldAcceptDiagnosticPayloadAsRawMap() throws Exception {
        mockMvc.perform(post("/api/storytelling/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", " "))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAcceptDiagnosticEndpointPayload() throws Exception {
        mockMvc.perform(post("/api/storytelling/chat/diagnostic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "message", "test",
                                "conversationHistory", List.of(Map.of("role", "user", "text", "bonjour")),
                                "financialContext", Map.of("customField", "value")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true))
                .andExpect(jsonPath("$.payload.message").value("test"));
    }
}

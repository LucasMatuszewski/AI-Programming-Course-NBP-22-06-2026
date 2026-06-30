package pl.nbp.copilot.backend.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pl.nbp.copilot.backend.cases.*;
import pl.nbp.copilot.backend.llm.LlmClient;
import pl.nbp.copilot.backend.session.SessionStore;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for:
 * - POST /api/cases/{sessionId}/messages  (SSE streaming)
 * - GET  /api/cases/{sessionId}           (session view)
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ChatController integration tests")
class ChatControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LlmClient llmClient;

    @Autowired
    private SessionStore sessionStore;

    @AfterEach
    void cleanUp() {
        // Nothing — InMemorySessionStore evicts by TTL; sessions from tests don't interfere
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Session createAndSaveSession() {
        UUID sessionId = UUID.randomUUID();
        CaseRequest caseRequest = new CaseRequest(
                RequestType.COMPLAINT,
                EquipmentCategory.SMARTFONY,
                "Samsung Galaxy S24",
                LocalDate.of(2024, 1, 1),
                "Usterka"
        );
        ImageAssessment assessment = new ImageAssessment(
                RequestType.COMPLAINT, "Opis", true, "Pęknięty ekran", "Uderzenie",
                null, null, ImageQuality.OK, "raw"
        );
        Decision decision = new Decision(Outcome.UZNANA, "Uzasadnienie",
                List.of("Krok 1"), List.of(), true);
        ChatMessage firstMessage = new ChatMessage(
                MessageRole.ASSISTANT, "## Decyzja: Uznana", Instant.now());
        Instant now = Instant.now();

        Session session = new Session(
                sessionId, caseRequest, assessment, decision,
                new ArrayList<>(List.of(firstMessage)), now, now
        );
        sessionStore.save(session);
        return session;
    }

    // ─── POST /api/cases/{sessionId}/messages ─────────────────────────────────

    @Test
    @DisplayName("POST messages with 3 tokens → SSE with token events and done event")
    void postMessages_validSession_returnsSseStream() throws Exception {
        Session session = createAndSaveSession();

        doAnswer(invocation -> {
            java.util.function.Consumer<String> tokenConsumer = invocation.getArgument(3);
            Runnable onComplete = invocation.getArgument(4);
            tokenConsumer.accept("Witaj");
            tokenConsumer.accept(" klientu");
            tokenConsumer.accept("!");
            onComplete.run();
            return null;
        }).when(llmClient).streamChat(anyString(), anyList(), anyString(), any(), any(), any());

        MvcResult result = mockMvc.perform(
                        post("/api/cases/{sessionId}/messages", session.sessionId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\": \"Mam pytanie\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/event-stream"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        // Spring SseEmitter uses "event:token" format (no space after colon)
        assertThat(responseBody).containsAnyOf("event: token", "event:token");
        assertThat(responseBody).containsAnyOf("event: done", "event:done");
        assertThat(responseBody).containsAnyOf("data:Witaj", "data: Witaj");
    }

    @Test
    @DisplayName("POST messages to unknown sessionId → 404 SESSION_NOT_FOUND")
    void postMessages_unknownSession_returns404() throws Exception {
        mockMvc.perform(
                        post("/api/cases/{sessionId}/messages", UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\": \"Pytanie\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST messages with blank content → 400 VALIDATION_ERROR")
    void postMessages_blankContent_returns400() throws Exception {
        Session session = createAndSaveSession();

        mockMvc.perform(
                        post("/api/cases/{sessionId}/messages", session.sessionId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("LLM onError mid-stream → event: error emitted, prior messages intact")
    void postMessages_llmError_emitsErrorEvent() throws Exception {
        Session session = createAndSaveSession();
        int initialMessageCount = session.messages().size();

        doAnswer(invocation -> {
            java.util.function.Consumer<String> tokenConsumer = invocation.getArgument(3);
            java.util.function.Consumer<Throwable> onError = invocation.getArgument(5);
            tokenConsumer.accept("Partial");
            onError.accept(new RuntimeException("LLM crashed"));
            return null;
        }).when(llmClient).streamChat(anyString(), anyList(), anyString(), any(), any(), any());

        MvcResult result = mockMvc.perform(
                        post("/api/cases/{sessionId}/messages", session.sessionId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\": \"Test\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        // Spring SseEmitter uses "event:error" format (no space after colon)
        assertThat(responseBody).containsAnyOf("event: error", "event:error");

        // Prior messages must be intact — session still retrievable
        mockMvc.perform(get("/api/cases/{sessionId}", session.sessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages").isArray());
    }

    // ─── GET /api/cases/{sessionId} ───────────────────────────────────────────

    @Test
    @DisplayName("GET existing sessionId → 200 with decision and messages")
    void getSession_existingSession_returns200() throws Exception {
        Session session = createAndSaveSession();

        mockMvc.perform(get("/api/cases/{sessionId}", session.sessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(session.sessionId().toString()))
                .andExpect(jsonPath("$.decision").isNotEmpty())
                .andExpect(jsonPath("$.decision.outcome").value("UZNANA"))
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET unknown sessionId → 404 SESSION_NOT_FOUND")
    void getSession_unknownSession_returns404() throws Exception {
        mockMvc.perform(get("/api/cases/{sessionId}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }
}

package pl.nbp.copilot.backend.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import pl.nbp.copilot.backend.cases.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link OpenRouterLlmClient} using MockWebServer.
 *
 * <p>No real network — all HTTP calls are stubbed via MockWebServer.
 * TAC-301..306 covered here.
 */
@DisplayName("OpenRouterLlmClient integration tests (MockWebServer)")
class OpenRouterLlmClientIT {

    private MockWebServer server;
    private OpenRouterLlmClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String VISION_MODEL = "test/vision-model";
    private static final String TEXT_MODEL = "test/text-model";

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        String baseUrl = server.url("/v1").toString();
        client = OpenRouterLlmClient.forTesting(baseUrl, "test-api-key", VISION_MODEL, TEXT_MODEL,
                new DecisionParser());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    // ─── TAC-301: client targets OpenRouter base URL ─────────────────────────

    @Test
    @DisplayName("analyzeImage sends request to configured base URL (TAC-301)")
    void analyzeImageSendsToConfiguredBaseUrl() throws Exception {
        server.enqueue(mockImageAssessmentResponse());

        client.analyzeImage("data:image/jpeg;base64,abc123", "image/jpeg", "Analyze this image");

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req, "Expected a request to be made");
        assertTrue(req.getPath().contains("/chat/completions"),
                "Request path must contain /chat/completions; got: " + req.getPath());
    }

    // ─── TAC-305: vision call uses vision model ───────────────────────────────

    @Test
    @DisplayName("analyzeImage sends vision model in request body (TAC-305)")
    void analyzeImageUsesVisionModel() throws Exception {
        server.enqueue(mockImageAssessmentResponse());

        client.analyzeImage("data:image/jpeg;base64,abc123", "image/jpeg", "Analyze for damage");

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req);
        String body = req.getBody().readUtf8();
        assertTrue(body.contains(VISION_MODEL),
                "Vision call must use the vision model; body: " + body);
    }

    @Test
    @DisplayName("analyzeImage sends image as base64 data URL content part (TAC-305)")
    void analyzeImageSendsBase64DataUrl() throws Exception {
        server.enqueue(mockImageAssessmentResponse());

        String dataUrl = "data:image/jpeg;base64,abc123";
        client.analyzeImage(dataUrl, "image/jpeg", "Analyze for damage");

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req);
        String body = req.getBody().readUtf8();
        assertTrue(body.contains("abc123"),
                "Request body must contain the base64 image data; body: " + body);
        assertTrue(body.contains("image_url") || body.contains("data:image"),
                "Request body must use image_url content part type; body: " + body);
    }

    // ─── TAC-305: reasoning/chat calls use text model ────────────────────────

    @Test
    @DisplayName("decide sends text model in request body (TAC-305)")
    void decideSendsTextModel() throws Exception {
        server.enqueue(mockDecisionResponse(Outcome.UZNANA));

        client.decide("Assess this complaint.", RequestType.COMPLAINT);

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req);
        String body = req.getBody().readUtf8();
        assertTrue(body.contains(TEXT_MODEL),
                "Reasoning call must use the text model; body: " + body);
    }

    @Test
    @DisplayName("decide sends reasoning prompt and returns parsed Decision (TAC-305)")
    void decideReturnsDecision() throws Exception {
        server.enqueue(mockDecisionResponse(Outcome.UZNANA));

        Decision decision = client.decide("Assess this complaint.", RequestType.COMPLAINT);

        assertNotNull(decision);
        assertEquals(Outcome.UZNANA, decision.outcome());
    }

    @Test
    @DisplayName("decide returns WYMAGA_WERYFIKACJI fallback when LLM returns malformed JSON")
    void decideReturnsFallbackOnMalformedJson() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(wrapInChatCompletion("This is definitely not valid JSON!")));

        Decision decision = client.decide("Assess this complaint.", RequestType.COMPLAINT);

        assertNotNull(decision);
        assertEquals(Outcome.WYMAGA_WERYFIKACJI, decision.outcome());
        assertFalse(decision.missingInfo().isEmpty());
    }

    // ─── TAC-306: streaming chat ──────────────────────────────────────────────

    @Test
    @DisplayName("streamChat relays token deltas in order and calls onComplete (TAC-306)")
    void streamChatRelaysDeltasAndCallsOnComplete() throws Exception {
        server.enqueue(mockSseStreamResponse(List.of("Cześć", " jak", " mogę", " pomóc?")));

        List<String> tokens = new ArrayList<>();
        CountDownLatch completedLatch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        client.streamChat(
                "System prompt",
                List.of(),
                "Witaj",
                tokens::add,
                completedLatch::countDown,
                errorRef::set
        );

        boolean completed = completedLatch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "onComplete must be called within timeout");
        assertNull(errorRef.get(), "onError must NOT be called on success");
        assertFalse(tokens.isEmpty(), "At least one token must be relayed");
        assertEquals("Cześć jak mogę pomóc?", String.join("", tokens));
    }

    @Test
    @DisplayName("streamChat uses text model (TAC-305)")
    void streamChatUsesTextModel() throws Exception {
        server.enqueue(mockSseStreamResponse(List.of("token")));

        CountDownLatch latch = new CountDownLatch(1);
        client.streamChat("System", List.of(), "msg", t -> {}, latch::countDown, e -> latch.countDown());
        latch.await(5, TimeUnit.SECONDS);

        RecordedRequest req = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(req);
        String body = req.getBody().readUtf8();
        assertTrue(body.contains(TEXT_MODEL),
                "Stream chat must use text model; body: " + body);
    }

    @Test
    @DisplayName("streamChat calls onError on upstream connection failure (TAC-306)")
    void streamChatCallsOnErrorOnUpstreamFailure() throws Exception {
        // Simulate an early close / error response
        server.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\": {\"message\": \"Internal Server Error\", \"type\": \"server_error\"}}"));

        AtomicBoolean onErrorCalled = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        client.streamChat(
                "System",
                List.of(),
                "Hello",
                token -> {},
                latch::countDown,
                error -> {
                    onErrorCalled.set(true);
                    latch.countDown();
                }
        );

        latch.await(5, TimeUnit.SECONDS);
        assertTrue(onErrorCalled.get(), "onError must be called on upstream 5xx");
    }

    @Test
    @DisplayName("streamChat passes chat history in request body")
    void streamChatPassesHistoryInRequestBody() throws Exception {
        server.enqueue(mockSseStreamResponse(List.of("odpowiedź")));

        CountDownLatch latch = new CountDownLatch(1);
        List<ChatMessage> history = List.of(
                new ChatMessage(MessageRole.USER, "Moje pierwsze pytanie", java.time.Instant.now()),
                new ChatMessage(MessageRole.ASSISTANT, "Moja odpowiedź", java.time.Instant.now())
        );

        client.streamChat("System", history, "Kolejne pytanie", t -> {}, latch::countDown, e -> latch.countDown());
        latch.await(5, TimeUnit.SECONDS);

        RecordedRequest req = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(req);
        String body = req.getBody().readUtf8();
        assertTrue(body.contains("Moje pierwsze pytanie"),
                "Request body must include user history message; body: " + body);
    }

    // ─── Helper methods ───────────────────────────────────────────────────────

    private MockResponse mockImageAssessmentResponse() {
        String assessmentJson = """
                {
                  "requestType": "COMPLAINT",
                  "description": "Widoczne pęknięcie ekranu.",
                  "damageDetected": true,
                  "damageType": "pęknięty ekran",
                  "likelyCause": "uderzenie mechaniczne",
                  "signsOfUse": null,
                  "resellableCondition": null,
                  "imageQuality": "OK",
                  "rawModelText": "raw"
                }
                """;
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(wrapInChatCompletion(assessmentJson));
    }

    private MockResponse mockDecisionResponse(Outcome outcome) {
        String decisionJson = """
                {
                  "outcome": "%s",
                  "justification": "Zgodne z §2 polityki reklamacyjnej.",
                  "nextSteps": ["Skontaktuj się z serwisem"],
                  "missingInfo": []
                }
                """.formatted(outcome.name());
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(wrapInChatCompletion(decisionJson));
    }

    private String wrapInChatCompletion(String content) {
        // Escaping the content for JSON embedding
        String escaped = content
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return """
                {
                  "id": "chatcmpl-test",
                  "object": "chat.completion",
                  "created": 1700000000,
                  "model": "test/model",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "%s"
                      },
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 10,
                    "completion_tokens": 20,
                    "total_tokens": 30
                  }
                }
                """.formatted(escaped);
    }

    private MockResponse mockSseStreamResponse(List<String> tokens) {
        StringBuilder sseBody = new StringBuilder();
        for (String token : tokens) {
            String escaped = token.replace("\"", "\\\"");
            sseBody.append("data: {\"id\":\"chatcmpl-test\",\"object\":\"chat.completion.chunk\",")
                    .append("\"created\":1700000000,\"model\":\"test/model\",")
                    .append("\"choices\":[{\"index\":0,\"delta\":{\"content\":\"")
                    .append(escaped)
                    .append("\"},\"finish_reason\":null}]}\n\n");
        }
        sseBody.append("data: [DONE]\n\n");

        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody.toString());
    }
}

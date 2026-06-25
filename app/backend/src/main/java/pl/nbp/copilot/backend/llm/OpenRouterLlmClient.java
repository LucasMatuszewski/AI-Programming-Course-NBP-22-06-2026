package pl.nbp.copilot.backend.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.nbp.copilot.backend.cases.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * {@link LlmClient} implementation that calls OpenRouter via the
 * {@code com.openai:openai-java} SDK (ADR-003).
 *
 * <p>Three call types:
 * <ol>
 *   <li>Vision ({@link #analyzeImage}) — sends a multimodal message with a base64 data-URL
 *       image content part; returns a parsed {@link ImageAssessment}.</li>
 *   <li>Reasoning ({@link #decide}) — sends the reasoning prompt; parses the JSON
 *       response via {@link DecisionParser}; returns a {@link Decision}.</li>
 *   <li>Streaming chat ({@link #streamChat}) — streams token deltas to a callback.</li>
 * </ol>
 *
 * <p>All SDK types are confined to this class; callers depend only on {@link LlmClient}.
 */
@Component
public class OpenRouterLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterLlmClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final OpenAIClient openAIClient;
    private final ModelSelector modelSelector;
    private final DecisionParser decisionParser;

    /**
     * Creates the client with its required collaborators (constructor injection).
     *
     * @param openAIClient   configured SDK client pointing at OpenRouter
     * @param modelSelector  provides vision/text model IDs from config
     * @param decisionParser parses LLM JSON responses into {@link Decision} objects
     */
    public OpenRouterLlmClient(
            OpenAIClient openAIClient,
            ModelSelector modelSelector,
            DecisionParser decisionParser
    ) {
        this.openAIClient = openAIClient;
        this.modelSelector = modelSelector;
        this.decisionParser = decisionParser;
    }

    // ─── Package-private factory for tests (avoids full Spring context) ───────

    /**
     * Creates a test instance pointing at a custom base URL.
     * Used only by integration tests with MockWebServer — not for production use.
     *
     * @param baseUrl        MockWebServer base URL
     * @param apiKey         dummy test API key
     * @param visionModel    vision model ID to use in test
     * @param textModel      text model ID to use in test
     * @param decisionParser parser instance
     * @return a configured test client
     */
    static OpenRouterLlmClient forTesting(
            String baseUrl,
            String apiKey,
            String visionModel,
            String textModel,
            DecisionParser decisionParser
    ) {
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .maxRetries(0)
                .build();

        // Create a ModelSelector-like wrapper using anonymous subclass for tests
        ModelSelector testSelector = new ModelSelector(visionModel, textModel) {
            // All fields inherited, just different values via constructor
        };

        return new OpenRouterLlmClient(client, testSelector, decisionParser);
    }

    // ─── LlmClient implementation ─────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Sends a user message with two content parts: the image prompt text + the
     * base64 data-URL image. Uses the vision model. On parse failure, returns
     * an assessment with {@code imageQuality=POOR_UNREADABLE}.
     */
    @Override
    public ImageAssessment analyzeImage(String base64DataUrl, String mimeType, String imagePrompt) {
        log.debug("analyzeImage: calling vision model={}", modelSelector.visionModel());
        try {
            ChatCompletionContentPartText textPart = ChatCompletionContentPartText.builder()
                    .text(imagePrompt)
                    .build();

            ChatCompletionContentPartImage imagePart = ChatCompletionContentPartImage.builder()
                    .imageUrl(ChatCompletionContentPartImage.ImageUrl.builder()
                            .url(base64DataUrl)
                            .build())
                    .build();

            List<ChatCompletionContentPart> contentParts = List.of(
                    ChatCompletionContentPart.ofText(textPart),
                    ChatCompletionContentPart.ofImageUrl(imagePart)
            );

            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(modelSelector.visionModel())
                    .addUserMessageOfArrayOfContentParts(contentParts)
                    .build();

            ChatCompletion completion = openAIClient.chat().completions().create(params);
            String rawContent = extractContent(completion);

            return parseImageAssessment(rawContent);

        } catch (Exception e) {
            log.error("analyzeImage: error calling vision model", e);
            return poorUnreadableFallback();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sends the reasoning prompt as a user message. Uses the text model.
     * Parses the JSON response via {@link DecisionParser}; any parse failure
     * returns the safe fallback (never throws for model formatting issues).
     */
    @Override
    public Decision decide(String reasoningPrompt, RequestType requestType) {
        log.debug("decide: calling text model={}", modelSelector.textModel());
        try {
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(modelSelector.textModel())
                    .addUserMessage(reasoningPrompt)
                    .build();

            ChatCompletion completion = openAIClient.chat().completions().create(params);
            String rawContent = extractContent(completion);

            return decisionParser.parse(rawContent, requestType);

        } catch (Exception e) {
            log.error("decide: error calling text model", e);
            throw new LlmUpstreamException("Błąd komunikacji z modelem decyzyjnym", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Builds the full chat message list (system + history + user), calls
     * {@code createStreaming}, relays non-null delta tokens to {@code tokenConsumer},
     * calls {@code onComplete} after the last chunk, and {@code onError} on failure.
     *
     * <p>Runs on a virtual thread so the caller is not blocked.
     */
    @Override
    public void streamChat(
            String systemPrompt,
            List<ChatMessage> history,
            String userMessage,
            Consumer<String> tokenConsumer,
            Runnable onComplete,
            Consumer<Throwable> onError
    ) {
        log.debug("streamChat: calling text model={}", modelSelector.textModel());

        Thread.ofVirtual().start(() -> {
            try {
                ChatCompletionCreateParams params = buildChatParams(systemPrompt, history, userMessage);

                try (StreamResponse<ChatCompletionChunk> stream =
                             openAIClient.chat().completions().createStreaming(params)) {
                    stream.stream().forEach(chunk -> {
                        for (ChatCompletionChunk.Choice choice : chunk.choices()) {
                            choice.delta().content().ifPresent(content -> {
                                if (!content.isEmpty()) {
                                    tokenConsumer.accept(content);
                                }
                            });
                        }
                    });
                }
                onComplete.run();

            } catch (Exception e) {
                log.error("streamChat: error during streaming", e);
                onError.accept(e);
            }
        });
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private ChatCompletionCreateParams buildChatParams(
            String systemPrompt, List<ChatMessage> history, String userMessage) {

        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
                .model(modelSelector.textModel())
                .addSystemMessage(systemPrompt);

        for (ChatMessage msg : history) {
            switch (msg.role()) {
                case USER -> builder.addUserMessage(msg.content());
                case ASSISTANT -> builder.addAssistantMessage(msg.content());
                case SYSTEM -> builder.addSystemMessage(msg.content());
            }
        }

        builder.addUserMessage(userMessage);
        return builder.build();
    }

    private String extractContent(ChatCompletion completion) {
        return completion.choices().stream()
                .findFirst()
                .map(choice -> choice.message().content().orElse(""))
                .orElse("");
    }

    private ImageAssessment parseImageAssessment(String rawContent) {
        try {
            ImageAssessmentDto dto = OBJECT_MAPPER.readValue(rawContent, ImageAssessmentDto.class);
            return new ImageAssessment(
                    dto.requestType != null ? RequestType.valueOf(dto.requestType) : null,
                    dto.description,
                    dto.damageDetected,
                    dto.damageType,
                    dto.likelyCause,
                    dto.signsOfUse,
                    dto.resellableCondition,
                    dto.imageQuality != null
                            ? ImageQuality.valueOf(dto.imageQuality)
                            : ImageQuality.POOR_UNREADABLE,
                    rawContent
            );
        } catch (Exception e) {
            log.warn("parseImageAssessment: failed to parse model response; returning POOR_UNREADABLE fallback");
            return poorUnreadableFallback();
        }
    }

    private ImageAssessment poorUnreadableFallback() {
        return new ImageAssessment(
                null,
                "Nie udało się odczytać zdjęcia.",
                null,
                null,
                null,
                null,
                null,
                ImageQuality.POOR_UNREADABLE,
                ""
        );
    }

    /**
     * Internal DTO for deserializing the image assessment JSON from the vision model.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ImageAssessmentDto {
        public String requestType;
        public String description;
        public Boolean damageDetected;
        public String damageType;
        public String likelyCause;
        public String signsOfUse;
        public Boolean resellableCondition;
        public String imageQuality;
        public String rawModelText;
    }
}

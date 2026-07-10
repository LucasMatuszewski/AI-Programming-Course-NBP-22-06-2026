package pl.nbp.copilot.backend.llm;

import pl.nbp.copilot.backend.cases.ChatMessage;
import pl.nbp.copilot.backend.cases.Decision;
import pl.nbp.copilot.backend.cases.ImageAssessment;
import pl.nbp.copilot.backend.cases.RequestType;

import java.util.List;
import java.util.function.Consumer;

/**
 * App-facing LLM abstraction.
 *
 * <p>Hides the {@code com.openai:openai-java} SDK types so that the rest of the
 * application does not depend on them. This isolation allows switching providers
 * or migrating the SDK without changing callers (ADR-003 §3).
 *
 * <p>All three methods are synchronous from the caller's perspective; streaming is
 * delivered via callbacks ({@code tokenConsumer}, {@code onComplete}, {@code onError}).
 */
public interface LlmClient {

    /**
     * Calls the vision LLM stage: analyzes a base64-encoded image and returns a
     * structured {@link ImageAssessment}.
     *
     * <p>On a non-parseable or unreachable upstream, the implementation must return
     * an assessment with {@code imageQuality=POOR_UNREADABLE} rather than throwing.
     *
     * @param base64DataUrl full data URL ({@code data:image/<type>;base64,...})
     * @param mimeType      MIME type of the image (e.g. {@code image/jpeg})
     * @param imagePrompt   scenario-specific image inspection prompt text
     * @return the parsed image assessment (never null)
     * @throws LlmUpstreamException on a non-5xx-recoverable upstream error
     * @throws LlmTimeoutException  on a request timeout
     */
    ImageAssessment analyzeImage(String base64DataUrl, String mimeType, String imagePrompt);

    /**
     * Calls the reasoning LLM stage: sends the reasoning prompt and parses the
     * structured JSON {@link Decision}.
     *
     * <p>On a non-parseable response the implementation delegates to {@link DecisionParser}
     * which returns a safe {@code WYMAGA_WERYFIKACJI} fallback — never throws for model
     * formatting issues.
     *
     * @param reasoningPrompt the assembled reasoning prompt with policy + form + assessment
     * @param requestType     the scenario used to validate the outcome enum
     * @return the parsed decision (never null)
     * @throws LlmUpstreamException on a non-5xx-recoverable upstream error
     * @throws LlmTimeoutException  on a request timeout
     */
    Decision decide(String reasoningPrompt, RequestType requestType);

    /**
     * Starts a streaming chat turn, delivering token deltas to {@code tokenConsumer}.
     *
     * <p>Runs the streaming call on a background thread; the caller must not block
     * on the same thread. Callbacks are invoked on the streaming thread.
     *
     * @param systemPrompt    chat system prompt (persona + context)
     * @param history         prior conversation messages (USER / ASSISTANT only)
     * @param userMessage     new user message text
     * @param tokenConsumer   called once per received delta token (non-null content)
     * @param onComplete      called after the last token or {@code [DONE]} sentinel
     * @param onError         called if an error or upstream close is detected
     */
    void streamChat(
            String systemPrompt,
            List<ChatMessage> history,
            String userMessage,
            Consumer<String> tokenConsumer,
            Runnable onComplete,
            Consumer<Throwable> onError
    );
}

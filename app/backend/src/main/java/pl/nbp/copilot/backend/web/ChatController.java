package pl.nbp.copilot.backend.web;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pl.nbp.copilot.backend.cases.*;
import pl.nbp.copilot.backend.llm.LlmClient;
import pl.nbp.copilot.backend.llm.PromptFactory;
import pl.nbp.copilot.backend.policy.PolicyProvider;
import pl.nbp.copilot.backend.session.SessionStore;

import java.time.Instant;
import java.util.UUID;

/**
 * REST controller for:
 * <ul>
 *   <li>{@code POST /api/cases/{sessionId}/messages} — SSE streaming chat turn</li>
 *   <li>{@code GET  /api/cases/{sessionId}} — session view</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/cases")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final SessionStore sessionStore;
    private final LlmClient llmClient;
    private final PromptFactory promptFactory;
    private final PolicyProvider policyProvider;
    private final long sseTimeoutMs;

    /**
     * Constructor injection.
     *
     * @param sessionStore    session storage
     * @param llmClient       LLM abstraction
     * @param promptFactory   builds the chat system prompt
     * @param policyProvider  loads the policy document
     * @param sseTimeoutMs    SSE emitter timeout in milliseconds (configurable)
     */
    public ChatController(
            SessionStore sessionStore,
            LlmClient llmClient,
            PromptFactory promptFactory,
            PolicyProvider policyProvider,
            @Value("${app.sse.timeout-ms:120000}") long sseTimeoutMs
    ) {
        this.sessionStore = sessionStore;
        this.llmClient = llmClient;
        this.promptFactory = promptFactory;
        this.policyProvider = policyProvider;
        this.sseTimeoutMs = sseTimeoutMs;
    }

    /**
     * Streams a chat response as Server-Sent Events.
     *
     * <p>The user message is appended to the session history, the LLM streams token deltas
     * as {@code event: token} events, and the assembled response is appended as an
     * ASSISTANT message on completion.
     *
     * @param sessionId the session to continue
     * @param body      the user message
     * @return {@link SseEmitter} streaming {@code event: token} and {@code event: done}
     * @throws SessionNotFoundException if the session is not found or expired
     */
    @PostMapping(
            value = "/{sessionId}/messages",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter streamChat(
            @PathVariable UUID sessionId,
            @Valid @RequestBody ChatMessageDto body
    ) {
        Session session = sessionStore.get(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        // Append USER message immediately
        ChatMessage userMessage = new ChatMessage(MessageRole.USER, body.content(), Instant.now());
        Session sessionWithUser = session.withMessage(userMessage, Instant.now());
        sessionStore.save(sessionWithUser);

        // Build system prompt
        String policyText = policyProvider.load(session.caseRequest().requestType());
        CaseContext ctx = CaseContext.from(session.caseRequest());
        String systemPrompt = promptFactory.chatSystemPrompt(
                ctx, session.imageAssessment(), session.decision(), policyText);

        SseEmitter emitter = new SseEmitter(sseTimeoutMs);
        StringBuilder assistantBuffer = new StringBuilder();

        llmClient.streamChat(
                systemPrompt,
                sessionWithUser.messages(),
                body.content(),
                // tokenConsumer
                token -> {
                    assistantBuffer.append(token);
                    try {
                        emitter.send(SseEmitter.event()
                                .name("token")
                                .data(token));
                    } catch (Exception e) {
                        log.warn("SSE send failed for session {}: {}", sessionId, e.getMessage());
                        emitter.completeWithError(e);
                    }
                },
                // onComplete
                () -> {
                    try {
                        // Append the assembled ASSISTANT message
                        String assistantContent = assistantBuffer.toString();
                        ChatMessage assistantMsg = new ChatMessage(
                                MessageRole.ASSISTANT, assistantContent, Instant.now());
                        Session completed = sessionWithUser.withMessage(assistantMsg, Instant.now());
                        sessionStore.save(completed);

                        emitter.send(SseEmitter.event()
                                .name("done")
                                .data(""));
                        emitter.complete();
                        log.debug("SSE stream completed for session {}", sessionId);
                    } catch (Exception e) {
                        log.warn("SSE completion failed for session {}: {}", sessionId, e.getMessage());
                        emitter.completeWithError(e);
                    }
                },
                // onError
                error -> {
                    try {
                        log.error("LLM streaming error for session {}: {}", sessionId, error.getMessage());
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data("Wystąpił błąd podczas generowania odpowiedzi. Spróbuj ponownie."));
                        emitter.complete();
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                }
        );

        return emitter;
    }

    /**
     * Returns the session view: decision + full message history.
     *
     * @param sessionId the session to retrieve
     * @return 200 with {@link SessionViewDto}
     * @throws SessionNotFoundException if the session is not found or expired
     */
    @GetMapping("/{sessionId}")
    public SessionViewDto getSession(@PathVariable UUID sessionId) {
        Session session = sessionStore.get(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        return SessionViewDto.from(session);
    }
}

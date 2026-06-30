package pl.nbp.copilot.backend.cases;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.nbp.copilot.backend.image.ImageCompressor;
import pl.nbp.copilot.backend.image.ImageValidator;
import pl.nbp.copilot.backend.llm.LlmClient;
import pl.nbp.copilot.backend.llm.PromptFactory;
import pl.nbp.copilot.backend.policy.PolicyProvider;
import pl.nbp.copilot.backend.session.SessionStore;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the full case-creation pipeline (ADR-000 §6):
 * <ol>
 *   <li>Validate image (type and size)</li>
 *   <li>Compress image (resize + re-encode)</li>
 *   <li>Analyze image via LLM vision stage</li>
 *   <li>Load policy document</li>
 *   <li>Build reasoning prompt and call LLM reasoning stage</li>
 *   <li>Build first assistant message (Polish Markdown)</li>
 *   <li>Persist session with decision and first message; discard image bytes</li>
 * </ol>
 *
 * <p>If image validation fails, no LLM calls are made.
 */
@Service
public class CaseService {

    private static final Logger log = LoggerFactory.getLogger(CaseService.class);

    private final ImageValidator imageValidator;
    private final ImageCompressor imageCompressor;
    private final LlmClient llmClient;
    private final PolicyProvider policyProvider;
    private final PromptFactory promptFactory;
    private final DecisionMessageBuilder decisionMessageBuilder;
    private final SessionStore sessionStore;

    /**
     * Constructor injection of all dependencies.
     *
     * @param imageValidator        validates image MIME type and size
     * @param imageCompressor       compresses and resizes the image
     * @param llmClient             LLM abstraction for vision and reasoning calls
     * @param policyProvider        loads policy Markdown by request type
     * @param promptFactory         assembles LLM prompts
     * @param decisionMessageBuilder renders {@link Decision} as Polish Markdown
     * @param sessionStore          persists the case session
     */
    public CaseService(
            ImageValidator imageValidator,
            ImageCompressor imageCompressor,
            LlmClient llmClient,
            PolicyProvider policyProvider,
            PromptFactory promptFactory,
            DecisionMessageBuilder decisionMessageBuilder,
            SessionStore sessionStore
    ) {
        this.imageValidator = imageValidator;
        this.imageCompressor = imageCompressor;
        this.llmClient = llmClient;
        this.policyProvider = policyProvider;
        this.promptFactory = promptFactory;
        this.decisionMessageBuilder = decisionMessageBuilder;
        this.sessionStore = sessionStore;
    }

    /**
     * Runs the full case pipeline for a new customer submission.
     *
     * @param caseRequest intake form fields (validated, non-null)
     * @param imageBytes  raw image bytes — discarded after compression
     * @param mimeType    declared MIME type of the image
     * @return {@link CaseResult} with session ID, outcome, and structured decision
     * @throws pl.nbp.copilot.backend.image.UnsupportedImageTypeException if MIME type is invalid
     * @throws pl.nbp.copilot.backend.image.ImageTooLargeException         if image exceeds max size
     * @throws pl.nbp.copilot.backend.llm.LlmUpstreamException             on LLM upstream error
     * @throws pl.nbp.copilot.backend.llm.LlmTimeoutException              on LLM timeout
     */
    public CaseResult createCase(CaseRequest caseRequest, byte[] imageBytes, String mimeType) {
        // Step 1: Validate image — throws on error (zero LLM calls if invalid)
        imageValidator.validate(imageBytes, mimeType, "upload");

        // Step 2: Compress image
        var compressed = imageCompressor.compress(imageBytes, mimeType);
        log.debug("Image compressed: {} bytes → {} bytes", imageBytes.length, compressed.bytes().length);

        // Step 3: Analyze image via LLM vision stage
        PromptFactory.Prompt imagePromptPair = promptFactory.imagePrompt(caseRequest.requestType());
        ImageAssessment assessment = llmClient.analyzeImage(
                compressed.dataUrl(), compressed.contentType(), imagePromptPair.userText());
        log.debug("Image assessment: quality={}, damageDetected={}", assessment.imageQuality(), assessment.damageDetected());

        // Step 4: Load policy
        String policyText = policyProvider.load(caseRequest.requestType());

        // Step 5: Build reasoning prompt and call LLM reasoning stage
        CaseContext ctx = CaseContext.from(caseRequest);
        String reasoningPrompt = promptFactory.reasoningPrompt(
                caseRequest.requestType(), assessment, ctx, policyText);
        Decision decision = llmClient.decide(reasoningPrompt, caseRequest.requestType());
        log.debug("Decision outcome: {}", decision.outcome());

        // Step 6: Build first assistant message (Polish Markdown)
        String decisionMarkdown = decisionMessageBuilder.build(decision);

        // Step 7: Persist session with decision and first message (image bytes NOT stored)
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        ChatMessage firstMessage = new ChatMessage(MessageRole.ASSISTANT, decisionMarkdown, now);

        Session session = new Session(
                sessionId,
                caseRequest,
                assessment,
                decision,
                List.of(firstMessage),
                now,
                now
        );
        sessionStore.save(session);
        log.info("Session created: sessionId={}, outcome={}", sessionId, decision.outcome());

        return new CaseResult(sessionId, decision.outcome(), decisionMarkdown, decision);
    }
}

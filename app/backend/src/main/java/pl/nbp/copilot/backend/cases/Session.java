package pl.nbp.copilot.backend.cases;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * In-memory session holding all state for a single customer case.
 *
 * <p>Sessions are keyed by {@link #sessionId()} in the {@code SessionStore}.
 * They expire after {@code APP_SESSION_TTL_MINUTES} since {@link #lastAccessedAt()}.
 *
 * <p>Image bytes are NOT stored in the session (privacy + memory).
 * Only the derived {@link ImageAssessment} text is retained.
 *
 * @param sessionId       unique identifier (UUID)
 * @param caseRequest     form metadata — no raw image bytes
 * @param imageAssessment structured output of the multimodal stage
 * @param decision        structured output of the reasoning stage
 * @param messages        chronological chat history; first element is the SYSTEM decision message
 * @param createdAt       when the session was created
 * @param lastAccessedAt  updated on every read; used for TTL eviction
 */
public record Session(
        UUID sessionId,
        CaseRequest caseRequest,
        ImageAssessment imageAssessment,
        Decision decision,
        List<ChatMessage> messages,
        Instant createdAt,
        Instant lastAccessedAt
) {

    /**
     * Returns a new {@code Session} with an updated {@code lastAccessedAt} timestamp.
     *
     * @param accessedAt the new last-accessed timestamp
     * @return new immutable session with refreshed timestamp
     */
    public Session withLastAccessedAt(Instant accessedAt) {
        return new Session(sessionId, caseRequest, imageAssessment, decision, messages, createdAt, accessedAt);
    }

    /**
     * Returns a new {@code Session} with an additional message appended.
     *
     * @param message      the message to append
     * @param accessedAt   the new last-accessed timestamp
     * @return new immutable session with the message appended
     */
    public Session withMessage(ChatMessage message, Instant accessedAt) {
        var newMessages = new java.util.ArrayList<>(messages);
        newMessages.add(message);
        return new Session(sessionId, caseRequest, imageAssessment, decision,
                java.util.Collections.unmodifiableList(newMessages), createdAt, accessedAt);
    }
}

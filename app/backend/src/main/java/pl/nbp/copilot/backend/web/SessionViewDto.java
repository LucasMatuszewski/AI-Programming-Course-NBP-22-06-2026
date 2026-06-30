package pl.nbp.copilot.backend.web;

import pl.nbp.copilot.backend.cases.ChatMessage;
import pl.nbp.copilot.backend.cases.MessageRole;
import pl.nbp.copilot.backend.cases.Session;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for {@code GET /api/cases/{sessionId}}.
 *
 * @param sessionId unique identifier of the session
 * @param decision  structured decision
 * @param messages  chronological chat history
 */
public record SessionViewDto(
        UUID sessionId,
        DecisionDto decision,
        List<MessageViewDto> messages
) {

    /**
     * Single message in the session view.
     *
     * @param role      SYSTEM | USER | ASSISTANT
     * @param content   message text in Polish Markdown
     * @param createdAt wall-clock timestamp
     */
    public record MessageViewDto(
            MessageRole role,
            String content,
            Instant createdAt
    ) {
        /**
         * Converts a {@link ChatMessage} domain object to a {@code MessageViewDto}.
         *
         * @param msg the domain message
         * @return a new {@code MessageViewDto}
         */
        public static MessageViewDto from(ChatMessage msg) {
            return new MessageViewDto(msg.role(), msg.content(), msg.createdAt());
        }
    }

    /**
     * Converts a {@link Session} domain object to a {@code SessionViewDto}.
     *
     * @param session the session domain object
     * @return a new {@code SessionViewDto}
     */
    public static SessionViewDto from(Session session) {
        List<MessageViewDto> messages = session.messages().stream()
                .map(MessageViewDto::from)
                .toList();
        return new SessionViewDto(
                session.sessionId(),
                DecisionDto.from(session.decision()),
                messages
        );
    }
}

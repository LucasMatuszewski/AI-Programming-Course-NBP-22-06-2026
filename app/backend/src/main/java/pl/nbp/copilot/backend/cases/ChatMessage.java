package pl.nbp.copilot.backend.cases;

import java.time.Instant;

/**
 * Single message in the chat history attached to a {@link Session}.
 *
 * <p>The first message always has role {@link MessageRole#SYSTEM} and contains the
 * formatted decision produced by {@code DecisionMessageBuilder}.
 *
 * @param role      SYSTEM | USER | ASSISTANT
 * @param content   markdown-formatted message body
 * @param createdAt wall-clock timestamp of message creation
 */
public record ChatMessage(
        MessageRole role,
        String content,
        Instant createdAt
) {
}

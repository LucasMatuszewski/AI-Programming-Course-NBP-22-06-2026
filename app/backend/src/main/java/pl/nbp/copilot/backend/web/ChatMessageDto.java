package pl.nbp.copilot.backend.web;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/cases/{sessionId}/messages}.
 *
 * @param content the user's chat message — must not be blank
 */
public record ChatMessageDto(
        @NotBlank(message = "Treść wiadomości nie może być pusta.")
        String content
) {
}

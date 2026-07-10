/**
 * LLM client layer: thin wrapper around the {@code com.openai:openai-java} SDK.
 *
 * <p>Responsibility: Encapsulates the openai-java SDK types behind a {@code LlmClient}
 * interface so no other package depends on SDK types directly (per ADR-003).
 * Handles vision calls, reasoning calls (structured JSON), and streaming chat calls.
 *
 * <p>The SDK client bean is configured in {@link pl.nbp.copilot.backend.config.AppProperties}
 * with the OpenRouter base URL override.
 *
 * <p>Depends on: {@code config}.
 * Depended on by: {@code cases}, {@code web} (ChatController for streaming).
 */
package pl.nbp.copilot.backend.llm;

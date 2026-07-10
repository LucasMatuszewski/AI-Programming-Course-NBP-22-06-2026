/**
 * Configuration layer: Spring beans and typed properties.
 *
 * <p>Responsibility:
 * <ul>
 *   <li>{@link pl.nbp.copilot.backend.config.AppProperties} — {@code @ConfigurationProperties("app")}
 *       binding of session TTL, image size limit, and CORS allowed origin.</li>
 *   <li>{@code OpenAiClientConfig} (future) — builds the openai-java SDK client bean
 *       pointed at OpenRouter with the correct API key.</li>
 *   <li>{@code WebConfig} (future) — CORS and multipart limits.</li>
 * </ul>
 *
 * <p>Depended on by: all other packages.
 */
package pl.nbp.copilot.backend.config;

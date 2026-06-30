package pl.nbp.copilot.backend.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration that builds the {@link OpenAIClient} bean.
 *
 * <p>The client is configured to call OpenRouter instead of the OpenAI API directly:
 * {@code baseUrl} is overridden to {@code OPENROUTER_BASE_URL} (default:
 * {@code https://openrouter.ai/api/v1}).
 *
 * <p>Key resolution (ADR-003 §3 / TAC-301):
 * {@code OPENAI_API_KEY} takes precedence over {@code OPENROUTER_API_KEY}
 * when both are set. If neither is set, startup fails fast.
 *
 * <p>Optional OpenRouter ranking headers ({@code HTTP-Referer} and {@code X-Title})
 * are added when present.
 */
@Configuration
public class OpenAiClientConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClientConfig.class);

    private final String openaiApiKey;
    private final String openrouterApiKey;
    private final String baseUrl;

    /**
     * Constructs the config with key values injected from application.yml / env vars.
     *
     * @param openaiApiKey     value of {@code OPENAI_API_KEY} (may be empty)
     * @param openrouterApiKey value of {@code OPENROUTER_API_KEY} (may be empty)
     * @param baseUrl          OpenRouter base URL; defaults to {@code https://openrouter.ai/api/v1}
     */
    public OpenAiClientConfig(
            @Value("${openai.api-key:}") String openaiApiKey,
            @Value("${openrouter.api-key:}") String openrouterApiKey,
            @Value("${openrouter.base-url:https://openrouter.ai/api/v1}") String baseUrl
    ) {
        this.openaiApiKey = openaiApiKey;
        this.openrouterApiKey = openrouterApiKey;
        this.baseUrl = baseUrl;
    }

    /**
     * Builds and exposes the {@link OpenAIClient} bean.
     *
     * <p>{@code OPENAI_API_KEY} is preferred; falls back to {@code OPENROUTER_API_KEY}.
     * Fails fast at startup (with a clear message) if neither is configured.
     *
     * @return the configured {@link OpenAIClient}
     * @throws IllegalStateException if no API key is available
     */
    @Bean
    public OpenAIClient openAIClient() {
        String resolvedKey = resolveApiKey();

        log.info("OpenAiClientConfig: building OpenAIClient with baseUrl={}", baseUrl);

        return OpenAIOkHttpClient.builder()
                .baseUrl(baseUrl)
                .apiKey(resolvedKey)
                .putHeader("HTTP-Referer", "https://github.com/nbp-copilot")
                .putHeader("X-Title", "Hardware Service Decision Copilot")
                .build();
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    /**
     * Resolves the API key with OPENAI_API_KEY taking precedence over OPENROUTER_API_KEY.
     *
     * @return the resolved key (non-blank)
     * @throws IllegalStateException if neither key is configured
     */
    private String resolveApiKey() {
        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            log.debug("OpenAiClientConfig: using OPENAI_API_KEY");
            return openaiApiKey;
        }
        if (openrouterApiKey != null && !openrouterApiKey.isBlank()) {
            log.debug("OpenAiClientConfig: using OPENROUTER_API_KEY");
            return openrouterApiKey;
        }
        throw new IllegalStateException(
                "Nie skonfigurowano klucza API do LLM. "
                + "Ustaw zmienną środowiskową OPENAI_API_KEY lub OPENROUTER_API_KEY.");
    }
}

package pl.nbp.copilot.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration for CORS, multipart limits, and async (SSE) timeout.
 *
 * <p>CORS is configured from {@link AppProperties#getCors()} so it can be overridden
 * via the {@code APP_CORS_ALLOWED_ORIGIN} environment variable.
 *
 * <p>Multipart limits are already declared in {@code application.yml}
 * ({@code spring.servlet.multipart.max-file-size/max-request-size}) and Spring Boot
 * auto-configuration handles them; this class extends them for programmatic use.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    /**
     * Constructor injection.
     *
     * @param appProperties typed application configuration
     */
    public WebConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * Configures CORS for all {@code /api/**} endpoints.
     *
     * <p>Allows origin from {@code app.cors.allowed-origin} (default {@code http://localhost:4200}),
     * methods GET/POST/OPTIONS, all headers, and credentials.
     *
     * @param registry the CORS registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(appProperties.getCors().getAllowedOrigin())
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    /**
     * Sets the async request timeout for SSE streaming to match the SSE emitter timeout
     * configured in {@code ChatController}.
     *
     * @param configurer the async support configurer
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // SSE timeout comes from app.sse.timeout-ms (default 120 s)
        // We set a slightly larger async timeout to avoid racing the emitter timeout
        configurer.setDefaultTimeout(130_000L);
    }
}

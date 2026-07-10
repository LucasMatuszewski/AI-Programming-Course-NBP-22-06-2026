package pl.nbp.copilot.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Typed binding for all {@code app.*} configuration values defined in
 * {@code application.yml}.
 *
 * <p>Registered via {@code @ConfigurationPropertiesScan} on
 * {@link pl.nbp.copilot.backend.BackendApplication}.
 *
 * <p>Environment-variable equivalents (Spring relaxed binding):
 * <ul>
 *   <li>{@code APP_SESSION_TTL_MINUTES} → {@code app.session.ttl-minutes}</li>
 *   <li>{@code APP_IMAGE_MAX_BYTES}     → {@code app.image.max-bytes}</li>
 *   <li>{@code APP_CORS_ALLOWED_ORIGIN} → {@code app.cors.allowed-origin}</li>
 * </ul>
 */
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Session session = new Session();
    private Image image = new Image();
    private Cors cors = new Cors();

    /** Returns session-related settings. */
    public Session getSession() {
        return session;
    }

    /** Sets session-related settings. */
    public void setSession(Session session) {
        this.session = session;
    }

    /** Returns image-related settings. */
    public Image getImage() {
        return image;
    }

    /** Sets image-related settings. */
    public void setImage(Image image) {
        this.image = image;
    }

    /** Returns CORS-related settings. */
    public Cors getCors() {
        return cors;
    }

    /** Sets CORS-related settings. */
    public void setCors(Cors cors) {
        this.cors = cors;
    }

    /**
     * Session TTL configuration.
     */
    public static class Session {

        /** In-memory session TTL in minutes. Default: 60. */
        @Positive
        private long ttlMinutes = 60;

        public long getTtlMinutes() {
            return ttlMinutes;
        }

        public void setTtlMinutes(long ttlMinutes) {
            this.ttlMinutes = ttlMinutes;
        }
    }

    /**
     * Image upload configuration.
     */
    public static class Image {

        /** Maximum accepted image upload size in bytes. Default: 10 MB. */
        @Positive
        private long maxBytes = 10_485_760L;

        public long getMaxBytes() {
            return maxBytes;
        }

        public void setMaxBytes(long maxBytes) {
            this.maxBytes = maxBytes;
        }
    }

    /**
     * CORS configuration.
     */
    public static class Cors {

        /** Allowed origin for the Angular frontend in dev. Default: http://localhost:4200. */
        @NotBlank
        private String allowedOrigin = "http://localhost:4200";

        public String getAllowedOrigin() {
            return allowedOrigin;
        }

        public void setAllowedOrigin(String allowedOrigin) {
            this.allowedOrigin = allowedOrigin;
        }
    }
}

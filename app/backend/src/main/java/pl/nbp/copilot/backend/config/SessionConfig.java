package pl.nbp.copilot.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import pl.nbp.copilot.backend.session.InMemorySessionStore;
import pl.nbp.copilot.backend.session.SessionStore;

import java.time.Clock;

/**
 * Spring configuration for session-related beans.
 *
 * <p>Provides a system {@link Clock} bean (injectable for tests to override) and
 * registers the {@link InMemorySessionStore} as the active {@link SessionStore}.
 */
@Configuration
@EnableScheduling
public class SessionConfig {

    private final AppProperties appProperties;

    /**
     * Creates the config with the application properties.
     *
     * @param appProperties typed configuration bean
     */
    public SessionConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * System UTC clock used by the session store for TTL tracking.
     *
     * @return system UTC clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * In-memory session store wired with the system clock and TTL from configuration.
     *
     * @param clock the injected clock bean
     * @return configured {@link InMemorySessionStore}
     */
    @Bean
    public SessionStore sessionStore(Clock clock) {
        return new InMemorySessionStore(clock, appProperties.getSession().getTtlMinutes());
    }
}

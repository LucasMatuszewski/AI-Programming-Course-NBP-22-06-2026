package pl.nbp.copilot.backend.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import pl.nbp.copilot.backend.cases.Session;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link SessionStore}.
 *
 * <p>Sessions are held in a {@link ConcurrentHashMap} keyed by session UUID.
 * A scheduled sweeper runs every minute to evict entries whose {@code lastAccessedAt}
 * is older than {@code ttlMinutes}.
 *
 * <p>The {@link Clock} dependency is injected so that tests can advance time without
 * sleeping.
 *
 * <p>State is lost on application restart. This is intentional for the MVP (ADR-000 §8).
 *
 * <p>Instantiated as a Spring bean via
 * {@link pl.nbp.copilot.backend.config.SessionConfig}.
 */
public class InMemorySessionStore implements SessionStore {

    private static final Logger log = LoggerFactory.getLogger(InMemorySessionStore.class);

    private final ConcurrentHashMap<UUID, Session> store = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration ttl;

    /**
     * Creates the store.
     *
     * @param clock      time source (injectable for testing)
     * @param ttlMinutes session TTL in minutes; sessions not accessed within this
     *                   window are evicted
     */
    public InMemorySessionStore(Clock clock, long ttlMinutes) {
        this.clock = clock;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    @Override
    public void save(Session session) {
        store.put(session.sessionId(), session);
        log.debug("Saved session {}", session.sessionId());
    }

    @Override
    public Optional<Session> get(UUID sessionId) {
        Session session = store.get(sessionId);
        if (session == null) {
            return Optional.empty();
        }

        // Refresh lastAccessedAt
        Session refreshed = session.withLastAccessedAt(clock.instant());
        store.put(sessionId, refreshed);
        return Optional.of(refreshed);
    }

    @Override
    @Scheduled(fixedDelay = 60_000)
    public void evictExpired() {
        var cutoff = clock.instant().minus(ttl);
        int[] count = {0};

        store.entrySet().removeIf(entry -> {
            boolean expired = !entry.getValue().lastAccessedAt().isAfter(cutoff);
            if (expired) {
                count[0]++;
                log.debug("Evicting expired session {} (lastAccessed={})",
                        entry.getKey(), entry.getValue().lastAccessedAt());
            }
            return expired;
        });

        if (count[0] > 0) {
            log.info("Evicted {} expired session(s)", count[0]);
        }
    }
}

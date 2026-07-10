package pl.nbp.copilot.backend.session;

import pl.nbp.copilot.backend.cases.Session;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for customer case sessions.
 *
 * <p>MVP implementation is in-memory ({@link InMemorySessionStore}).
 * The interface allows a persistent (database) implementation to drop in
 * for the Backlog phase without changing callers.
 *
 * <p>Implementations must:
 * <ul>
 *   <li>Refresh {@code lastAccessedAt} on every {@link #get(UUID)} call.</li>
 *   <li>Evict entries whose {@code lastAccessedAt} is older than the configured TTL.</li>
 *   <li>Never store raw image bytes (they are not part of {@link Session}).</li>
 * </ul>
 */
public interface SessionStore {

    /**
     * Saves (creates or replaces) a session.
     *
     * @param session the session to save
     */
    void save(Session session);

    /**
     * Retrieves a session by its ID and refreshes {@code lastAccessedAt}.
     *
     * @param sessionId the session identifier
     * @return the session if present and not yet expired, otherwise empty
     */
    Optional<Session> get(UUID sessionId);

    /**
     * Scans stored sessions and removes any whose {@code lastAccessedAt} is older than
     * the configured TTL. Called by a scheduled sweeper.
     */
    void evictExpired();
}

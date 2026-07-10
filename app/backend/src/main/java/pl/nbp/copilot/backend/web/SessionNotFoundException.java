package pl.nbp.copilot.backend.web;

import java.util.UUID;

/**
 * Thrown when a session ID is not found in the {@link pl.nbp.copilot.backend.session.SessionStore}
 * or has expired.
 *
 * <p>Maps to HTTP 404 SESSION_NOT_FOUND at the web layer.
 */
public class SessionNotFoundException extends RuntimeException {

    private final UUID sessionId;

    /**
     * Creates the exception.
     *
     * @param sessionId the session ID that was not found
     */
    public SessionNotFoundException(UUID sessionId) {
        super("Sesja nie została znaleziona lub wygasła: " + sessionId);
        this.sessionId = sessionId;
    }

    /**
     * Returns the missing session ID.
     *
     * @return session ID
     */
    public UUID getSessionId() {
        return sessionId;
    }
}

/**
 * In-memory session store.
 *
 * <p>Responsibility: {@code SessionStore} (interface) + {@code InMemorySessionStore}
 * hold the per-case session state (form data, image assessment, decision, chat messages)
 * in a {@code ConcurrentHashMap}. A scheduled sweeper evicts entries that exceed
 * {@code APP_SESSION_TTL_MINUTES} since their last access.
 *
 * <p>The interface allows the Backlog DB implementation to drop in without changing callers.
 *
 * <p>Depends on: nothing.
 * Depended on by: {@code web}, {@code cases}.
 */
package pl.nbp.copilot.backend.session;

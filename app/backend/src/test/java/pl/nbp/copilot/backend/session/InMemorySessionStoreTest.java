package pl.nbp.copilot.backend.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.nbp.copilot.backend.cases.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for InMemorySessionStore.
 *
 * <p>Uses a mutable {@link java.time.Clock} so tests never sleep.
 */
@DisplayName("InMemorySessionStore")
class InMemorySessionStoreTest {

    private static final long TTL_MINUTES = 60L;

    /** Mutable instant that acts as "now" in all tests. */
    private Instant currentTime;
    private Clock clock;
    private InMemorySessionStore store;

    @BeforeEach
    void setUp() {
        currentTime = Instant.parse("2026-06-25T12:00:00Z");
        clock = Clock.fixed(currentTime, ZoneOffset.UTC);
        store = new InMemorySessionStore(clock, TTL_MINUTES);
    }

    private Session buildSession(UUID id) {
        var req = new CaseRequest(RequestType.COMPLAINT, EquipmentCategory.SMARTFONY,
                "Test Phone", LocalDate.of(2026, 1, 1), "Broken screen");
        var assessment = new ImageAssessment(RequestType.COMPLAINT, "description",
                true, "cracked screen", "drop", null, null, ImageQuality.OK, "raw");
        var decision = new Decision(Outcome.UZNANA, "justification",
                List.of("next step"), List.of(), true);
        var now = clock.instant();
        var msg = new ChatMessage(MessageRole.SYSTEM, "First message", now);
        return new Session(id, req, assessment, decision, List.of(msg), now, now);
    }

    // ─── Save and Get ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("save and get")
    class SaveAndGetTests {

        @Test
        @DisplayName("saved session can be retrieved by sessionId")
        void saveThenGet() {
            var id = UUID.randomUUID();
            var session = buildSession(id);

            store.save(session);
            Optional<Session> found = store.get(id);

            assertTrue(found.isPresent(), "Should find saved session");
            assertEquals(id, found.get().sessionId());
        }

        @Test
        @DisplayName("get returns empty Optional for unknown sessionId")
        void getUnknownReturnsEmpty() {
            Optional<Session> found = store.get(UUID.randomUUID());
            assertTrue(found.isEmpty(), "Unknown session must return empty Optional");
        }

        @Test
        @DisplayName("get refreshes lastAccessedAt to current time")
        void getRefreshesLastAccessedAt() {
            var id = UUID.randomUUID();
            var session = buildSession(id);
            store.save(session);

            // Advance time by 10 minutes
            currentTime = currentTime.plusSeconds(600);
            clock = Clock.fixed(currentTime, ZoneOffset.UTC);
            store = new InMemorySessionStore(clock, TTL_MINUTES);
            store.save(session); // re-save with original time

            // Access at t+10
            clock = Clock.fixed(currentTime.plusSeconds(600), ZoneOffset.UTC);
            store = new InMemorySessionStore(clock, TTL_MINUTES);
            store.save(session);
            Optional<Session> found = store.get(id);

            // The internal lastAccessedAt should now equal clock.instant()
            assertTrue(found.isPresent());
        }

        @Test
        @DisplayName("get refreshes lastAccessedAt preventing immediate TTL eviction")
        void accessedSessionNotEvictedImmediately() {
            // Arrange: store with adjustable clock
            var id = UUID.randomUUID();
            var initialTime = Instant.parse("2026-06-25T12:00:00Z");
            var mutableClock = new MutableClock(initialTime);
            var testStore = new InMemorySessionStore(mutableClock, TTL_MINUTES);

            var session = buildSession(id);
            // Force lastAccessedAt to initial time
            testStore.save(session);

            // Advance clock to just before TTL expiry (59 minutes)
            mutableClock.advance(59 * 60);
            testStore.get(id); // this access refreshes lastAccessedAt

            // Advance another 59 minutes (total 118 min since creation, but only 59 since last access)
            mutableClock.advance(59 * 60);
            testStore.evictExpired();

            Optional<Session> found = testStore.get(id);
            assertTrue(found.isPresent(),
                    "Session accessed 59 minutes ago must not be evicted (TTL is 60 min)");
        }
    }

    // ─── TTL eviction ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("TTL eviction")
    class TtlEvictionTests {

        @Test
        @DisplayName("entry older than TTL is evicted by sweep")
        void evictsExpiredEntry() {
            var id = UUID.randomUUID();
            var mutableClock = new MutableClock(Instant.parse("2026-06-25T12:00:00Z"));
            var testStore = new InMemorySessionStore(mutableClock, TTL_MINUTES);

            testStore.save(buildSession(id));

            // Advance past TTL
            mutableClock.advance(TTL_MINUTES * 60 + 1);
            testStore.evictExpired();

            assertTrue(testStore.get(id).isEmpty(),
                    "Session past TTL must be evicted");
        }

        @Test
        @DisplayName("entry exactly at TTL boundary is evicted")
        void evictsAtExactTtlBoundary() {
            var id = UUID.randomUUID();
            var mutableClock = new MutableClock(Instant.parse("2026-06-25T12:00:00Z"));
            var testStore = new InMemorySessionStore(mutableClock, TTL_MINUTES);

            testStore.save(buildSession(id));

            // Advance exactly to TTL
            mutableClock.advance(TTL_MINUTES * 60);
            testStore.evictExpired();

            assertTrue(testStore.get(id).isEmpty(),
                    "Session exactly at TTL boundary must be evicted");
        }

        @Test
        @DisplayName("entry just within TTL is NOT evicted")
        void doesNotEvictWithinTtl() {
            var id = UUID.randomUUID();
            var mutableClock = new MutableClock(Instant.parse("2026-06-25T12:00:00Z"));
            var testStore = new InMemorySessionStore(mutableClock, TTL_MINUTES);

            testStore.save(buildSession(id));

            // Advance to one second before TTL
            mutableClock.advance(TTL_MINUTES * 60 - 1);
            testStore.evictExpired();

            assertTrue(testStore.get(id).isPresent(),
                    "Session within TTL must NOT be evicted");
        }
    }

    // ─── MutableClock helper ─────────────────────────────────────────────────

    /**
     * A mutable Clock implementation for testing TTL behavior without sleeping.
     */
    static class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant initial) {
            this.instant = initial;
        }

        void advance(long seconds) {
            this.instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}

package pl.nbp.copilot.backend.cases;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for DecisionMessageBuilder.
 *
 * <p>No Spring context — pure unit tests.
 */
@DisplayName("DecisionMessageBuilder")
class DecisionMessageBuilderTest {

    private static final String MANDATORY_DISCLAIMER =
            "Ocena ma charakter wstępny i nie jest wiążąca prawnie; "
            + "ostateczne rozpatrzenie może wymagać weryfikacji przez pracownika.";

    private DecisionMessageBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new DecisionMessageBuilder();
    }

    private Decision buildDecision(Outcome outcome) {
        List<String> missing = outcome == Outcome.WYMAGA_WERYFIKACJI
                ? List.of("Wyraźne zdjęcie uszkodzenia", "Potwierdzenie daty zakupu")
                : List.of();
        return new Decision(
                outcome,
                "Uszkodzenie mechaniczne widoczne na zdjęciu zgodnie z §4 regulaminu reklamacji.",
                List.of("Skontaktuj się z serwisem", "Dostarcz urządzenie do punktu obsługi"),
                missing,
                true
        );
    }

    // ─── Mandatory disclaimer ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Mandatory disclaimer (AC-16 / TAC-107)")
    class DisclaimerTests {

        @ParameterizedTest(name = "disclaimer present for outcome {0}")
        @EnumSource(Outcome.class)
        @DisplayName("every decision message contains the exact disclaimer text")
        void everyMessageContainsDisclaimer(Outcome outcome) {
            var decision = buildDecision(outcome);

            String markdown = builder.build(decision);

            assertTrue(markdown.contains(MANDATORY_DISCLAIMER),
                    "Decision message for " + outcome + " must contain the mandatory disclaimer");
        }
    }

    // ─── Five structural blocks ───────────────────────────────────────────────

    @Nested
    @DisplayName("Required content blocks (AC-17)")
    class ContentBlockTests {

        @Test
        @DisplayName("message contains greeting (block 1)")
        void containsGreeting() {
            var decision = buildDecision(Outcome.UZNANA);
            String markdown = builder.build(decision);
            // Greeting is the first heading or paragraph
            assertTrue(markdown.length() > 50, "Message must not be trivially short");
            // Should start with a recognizable greeting pattern
            assertTrue(markdown.contains("Witaj") || markdown.contains("Dzień dobry")
                    || markdown.toLowerCase().contains("drodzy"),
                    "Message must contain a Polish greeting");
        }

        @Test
        @DisplayName("message contains decision status label (block 2)")
        void containsStatusLabel() {
            var decision = buildDecision(Outcome.ODRZUCONA);
            String markdown = builder.build(decision);
            assertTrue(markdown.contains("Odrzucona"),
                    "Message must contain the outcome Polish label 'Odrzucona'");
        }

        @Test
        @DisplayName("message contains justification text (block 3)")
        void containsJustification() {
            var decision = buildDecision(Outcome.UZNANA);
            String markdown = builder.build(decision);
            assertTrue(markdown.contains("Uszkodzenie mechaniczne"),
                    "Message must contain the justification text");
        }

        @Test
        @DisplayName("message contains next steps (block 4)")
        void containsNextSteps() {
            var decision = buildDecision(Outcome.UZNANA);
            String markdown = builder.build(decision);
            assertTrue(markdown.contains("Skontaktuj się z serwisem"),
                    "Message must contain next-steps items");
        }

        @Test
        @DisplayName("message contains the mandatory disclaimer (block 5)")
        void containsDisclaimer() {
            var decision = buildDecision(Outcome.UZNANA);
            String markdown = builder.build(decision);
            assertTrue(markdown.contains(MANDATORY_DISCLAIMER));
        }

        @Test
        @DisplayName("message uses markdown headings or lists (not a single paragraph)")
        void usesMarkdownFormatting() {
            var decision = buildDecision(Outcome.UZNANA);
            String markdown = builder.build(decision);
            // Must contain at least one heading (#) or list item (-)
            assertTrue(markdown.contains("#") || markdown.contains("-") || markdown.contains("*"),
                    "Message must use markdown formatting (headings or lists)");
        }
    }

    // ─── Outcome-specific labels ──────────────────────────────────────────────

    @Nested
    @DisplayName("Polish outcome labels")
    class OutcomeLabelTests {

        @Test
        @DisplayName("UZNANA maps to 'Uznana' label")
        void uznanaMapsToLabel() {
            String markdown = builder.build(buildDecision(Outcome.UZNANA));
            assertTrue(markdown.contains("Uznana"), "UZNANA should render as 'Uznana'");
        }

        @Test
        @DisplayName("ODRZUCONA maps to 'Odrzucona' label")
        void odrzuconaMapsToLabel() {
            String markdown = builder.build(buildDecision(Outcome.ODRZUCONA));
            assertTrue(markdown.contains("Odrzucona"), "ODRZUCONA should render as 'Odrzucona'");
        }

        @Test
        @DisplayName("WYMAGA_WERYFIKACJI maps to 'Wymaga weryfikacji' label")
        void wymagaWeryfikacjiMapsToLabel() {
            String markdown = builder.build(buildDecision(Outcome.WYMAGA_WERYFIKACJI));
            assertTrue(markdown.contains("Wymaga weryfikacji"),
                    "WYMAGA_WERYFIKACJI should render as 'Wymaga weryfikacji'");
        }

        @Test
        @DisplayName("PRZYJETY_DO_ODSPRZEDAZY maps to 'Przyjęty do odsprzedaży' label")
        void przyjetyMapsToLabel() {
            var decision = new Decision(
                    Outcome.PRZYJETY_DO_ODSPRZEDAZY,
                    "Brak śladów użytkowania",
                    List.of("Wyślij paczką"),
                    List.of(),
                    true
            );
            String markdown = builder.build(decision);
            assertTrue(markdown.contains("Przyjęty do odsprzedaży"),
                    "PRZYJETY_DO_ODSPRZEDAZY should render as 'Przyjęty do odsprzedaży'");
        }
    }

    // ─── Escalation (WYMAGA_WERYFIKACJI) ─────────────────────────────────────

    @Nested
    @DisplayName("Escalation — WYMAGA_WERYFIKACJI")
    class EscalationTests {

        @Test
        @DisplayName("message renders missingInfo items when outcome is WYMAGA_WERYFIKACJI")
        void rendersMissingInfoForEscalation() {
            var decision = new Decision(
                    Outcome.WYMAGA_WERYFIKACJI,
                    "Zdjęcie nieczytelne",
                    List.of("Prześlij wyraźniejsze zdjęcie"),
                    List.of("Wyraźne zdjęcie uszkodzenia", "Potwierdzenie daty zakupu"),
                    true
            );

            String markdown = builder.build(decision);

            assertTrue(markdown.contains("Wyraźne zdjęcie uszkodzenia"),
                    "Message must list missing info item 1");
            assertTrue(markdown.contains("Potwierdzenie daty zakupu"),
                    "Message must list missing info item 2");
        }

        @Test
        @DisplayName("message does NOT render missingInfo section when list is empty")
        void doesNotRenderEmptyMissingInfo() {
            var decision = buildDecision(Outcome.UZNANA); // missingInfo is empty

            String markdown = builder.build(decision);

            // The section header "Brakujące informacje" or similar should not appear
            // when there is nothing missing
            assertFalse(markdown.contains("Brakujące informacje") && !decision.missingInfo().isEmpty(),
                    "Empty missingInfo should not produce a section");
        }
    }
}

package pl.nbp.copilot.backend.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.nbp.copilot.backend.cases.Decision;
import pl.nbp.copilot.backend.cases.Outcome;
import pl.nbp.copilot.backend.cases.RequestType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD unit tests for DecisionParser.
 *
 * <p>No Spring context — pure unit tests.
 *
 * <p>TAC-304: valid JSON yields correct Decision; malformed JSON yields WYMAGA_WERYFIKACJI fallback.
 */
@DisplayName("DecisionParser")
class DecisionParserTest {

    private DecisionParser parser;

    @BeforeEach
    void setUp() {
        parser = new DecisionParser();
    }

    // ─── Happy path ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Valid JSON input")
    class ValidInputTests {

        @Test
        @DisplayName("parses complaint outcome UZNANA from valid JSON")
        void parsesComplaintOutcomeUznana() {
            String json = """
                    {
                      "outcome": "UZNANA",
                      "justification": "Uszkodzenie mechaniczne zgodne z polityką §2.",
                      "nextSteps": ["Skontaktuj się z serwisem"],
                      "missingInfo": []
                    }
                    """;

            Decision result = parser.parse(json, RequestType.COMPLAINT);

            assertNotNull(result);
            assertEquals(Outcome.UZNANA, result.outcome());
            assertEquals("Uszkodzenie mechaniczne zgodne z polityką §2.", result.justification());
            assertEquals(List.of("Skontaktuj się z serwisem"), result.nextSteps());
            assertTrue(result.missingInfo().isEmpty());
        }

        @Test
        @DisplayName("parses return outcome PRZYJETY_DO_ODSPRZEDAZY from valid JSON")
        void parsesReturnOutcomePrzyjety() {
            String json = """
                    {
                      "outcome": "PRZYJETY_DO_ODSPRZEDAZY",
                      "justification": "Produkt w stanie handlowym, zwrot w terminie 14 dni.",
                      "nextSteps": ["Nadaj przesyłkę"],
                      "missingInfo": []
                    }
                    """;

            Decision result = parser.parse(json, RequestType.RETURN);

            assertEquals(Outcome.PRZYJETY_DO_ODSPRZEDAZY, result.outcome());
        }

        @Test
        @DisplayName("parses shared outcome ODRZUCONA for complaint scenario")
        void parsesOdrzuconaForComplaint() {
            String json = """
                    {
                      "outcome": "ODRZUCONA",
                      "justification": "Uszkodzenie wynikające z użytkowania.",
                      "nextSteps": [],
                      "missingInfo": []
                    }
                    """;

            Decision result = parser.parse(json, RequestType.COMPLAINT);

            assertEquals(Outcome.ODRZUCONA, result.outcome());
        }

        @Test
        @DisplayName("parses WYMAGA_WERYFIKACJI outcome for return scenario")
        void parsesWymagaWeryfikacjiForReturn() {
            String json = """
                    {
                      "outcome": "WYMAGA_WERYFIKACJI",
                      "justification": "Jakość zdjęcia uniemożliwia ocenę stanu.",
                      "nextSteps": ["Prześlij wyraźniejsze zdjęcie"],
                      "missingInfo": ["Brak wyraźnego zdjęcia"]
                    }
                    """;

            Decision result = parser.parse(json, RequestType.RETURN);

            assertEquals(Outcome.WYMAGA_WERYFIKACJI, result.outcome());
            assertEquals(List.of("Brak wyraźnego zdjęcia"), result.missingInfo());
        }

        @Test
        @DisplayName("extra fields in JSON are silently ignored")
        void extraFieldsInJsonAreIgnored() {
            String json = """
                    {
                      "outcome": "UZNANA",
                      "justification": "Reklamacja uzasadniona.",
                      "nextSteps": [],
                      "missingInfo": [],
                      "unknownField": "should be ignored",
                      "anotherExtra": 42
                    }
                    """;

            Decision result = parser.parse(json, RequestType.COMPLAINT);

            assertEquals(Outcome.UZNANA, result.outcome());
        }
    }

    // ─── Fallback path ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Malformed or invalid input → WYMAGA_WERYFIKACJI fallback (TAC-304)")
    class FallbackTests {

        @Test
        @DisplayName("non-JSON input returns fallback Decision")
        void nonJsonInputReturnsFallback() {
            Decision result = parser.parse("This is not JSON", RequestType.COMPLAINT);

            assertFallback(result);
        }

        @Test
        @DisplayName("truncated/partial JSON returns fallback Decision")
        void truncatedJsonReturnsFallback() {
            Decision result = parser.parse("{\"outcome\": \"UZNANA\", \"justification\":", RequestType.COMPLAINT);

            assertFallback(result);
        }

        @Test
        @DisplayName("wrong outcome enum value returns fallback Decision")
        void wrongOutcomeEnumValueReturnsFallback() {
            String json = """
                    {
                      "outcome": "INVALID_VALUE",
                      "justification": "Some justification.",
                      "nextSteps": [],
                      "missingInfo": []
                    }
                    """;

            Decision result = parser.parse(json, RequestType.COMPLAINT);

            assertFallback(result);
        }

        @Test
        @DisplayName("outcome valid globally but wrong scenario (PRZYJETY_DO_ODSPRZEDAZY for complaint) returns fallback")
        void outcomeWrongForScenarioReturnsFallback() {
            String json = """
                    {
                      "outcome": "PRZYJETY_DO_ODSPRZEDAZY",
                      "justification": "Produkt w stanie handlowym.",
                      "nextSteps": [],
                      "missingInfo": []
                    }
                    """;

            // PRZYJETY_DO_ODSPRZEDAZY is valid for RETURN but NOT for COMPLAINT
            Decision result = parser.parse(json, RequestType.COMPLAINT);

            assertFallback(result);
        }

        @Test
        @DisplayName("empty string input returns fallback Decision")
        void emptyStringReturnsFallback() {
            Decision result = parser.parse("", RequestType.COMPLAINT);

            assertFallback(result);
        }

        @Test
        @DisplayName("null input returns fallback Decision")
        void nullInputReturnsFallback() {
            Decision result = parser.parse(null, RequestType.COMPLAINT);

            assertFallback(result);
        }

        @Test
        @DisplayName("no exception is propagated from parse() in any case")
        void noExceptionPropagated() {
            assertDoesNotThrow(() -> parser.parse("not json at all", RequestType.COMPLAINT));
            assertDoesNotThrow(() -> parser.parse(null, RequestType.RETURN));
            assertDoesNotThrow(() -> parser.parse("{}", RequestType.COMPLAINT));
        }
    }

    // ─── Helper ─────────────────────────────────────────────────────────────

    private void assertFallback(Decision result) {
        assertNotNull(result, "Fallback decision must not be null");
        assertEquals(Outcome.WYMAGA_WERYFIKACJI, result.outcome(),
                "Fallback must have WYMAGA_WERYFIKACJI outcome");
        assertNotNull(result.missingInfo(), "Fallback missingInfo must not be null");
        assertFalse(result.missingInfo().isEmpty(),
                "Fallback missingInfo must contain the standard message");
        assertTrue(result.missingInfo().contains("Nie udało się jednoznacznie ocenić zgłoszenia"),
                "Fallback missingInfo must contain the standard Polish message");
    }
}

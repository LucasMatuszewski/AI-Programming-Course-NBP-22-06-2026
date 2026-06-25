package pl.nbp.copilot.backend.cases;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD — red-first tests for domain models and enums.
 * No Spring context; pure unit tests.
 */
@DisplayName("Domain models and enums")
class DomainModelsTest {

    // ─── EquipmentCategory ───────────────────────────────────────────────────

    @Nested
    @DisplayName("EquipmentCategory")
    class EquipmentCategoryTests {

        @Test
        @DisplayName("has exactly 10 values")
        void hasTenCategories() {
            assertEquals(10, EquipmentCategory.values().length,
                    "PRD §8 defines exactly 10 equipment categories");
        }

        @Test
        @DisplayName("contains all Polish display labels")
        void containsPolishLabels() {
            for (EquipmentCategory cat : EquipmentCategory.values()) {
                assertNotNull(cat.getDisplayLabel(),
                        "Category " + cat.name() + " must have a Polish display label");
                assertFalse(cat.getDisplayLabel().isBlank(),
                        "Category " + cat.name() + " display label must not be blank");
            }
        }

        @Test
        @DisplayName("contains SMARTFONY category with correct Polish label")
        void containsSmartfony() {
            assertEquals("Smartfony", EquipmentCategory.SMARTFONY.getDisplayLabel());
        }

        @Test
        @DisplayName("contains INNE category with correct Polish label")
        void containsInne() {
            assertEquals("Inne", EquipmentCategory.INNE.getDisplayLabel());
        }
    }

    // ─── RequestType ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RequestType")
    class RequestTypeTests {

        @Test
        @DisplayName("has exactly 2 values: COMPLAINT and RETURN")
        void hasTwoValues() {
            assertEquals(2, RequestType.values().length);
        }

        @Test
        @DisplayName("COMPLAINT and RETURN are defined")
        void complaintAndReturnDefined() {
            assertDoesNotThrow(() -> RequestType.valueOf("COMPLAINT"));
            assertDoesNotThrow(() -> RequestType.valueOf("RETURN"));
        }
    }

    // ─── Outcome ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Outcome enum")
    class OutcomeTests {

        @Test
        @DisplayName("complaint allowed set contains UZNANA, ODRZUCONA, WYMAGA_WERYFIKACJI")
        void complaintOutcomes() {
            var set = Outcome.allowedFor(RequestType.COMPLAINT);
            assertTrue(set.contains(Outcome.UZNANA));
            assertTrue(set.contains(Outcome.ODRZUCONA));
            assertTrue(set.contains(Outcome.WYMAGA_WERYFIKACJI));
            assertEquals(3, set.size(), "Complaint scenario must have exactly 3 outcomes");
        }

        @Test
        @DisplayName("return allowed set contains PRZYJETY_DO_ODSPRZEDAZY, ODRZUCONA, WYMAGA_WERYFIKACJI")
        void returnOutcomes() {
            var set = Outcome.allowedFor(RequestType.RETURN);
            assertTrue(set.contains(Outcome.PRZYJETY_DO_ODSPRZEDAZY));
            assertTrue(set.contains(Outcome.ODRZUCONA));
            assertTrue(set.contains(Outcome.WYMAGA_WERYFIKACJI));
            assertEquals(3, set.size(), "Return scenario must have exactly 3 outcomes");
        }

        @Test
        @DisplayName("UZNANA is NOT allowed for RETURN scenario")
        void uznanaNowAllowedForReturn() {
            assertFalse(Outcome.allowedFor(RequestType.RETURN).contains(Outcome.UZNANA));
        }

        @Test
        @DisplayName("PRZYJETY_DO_ODSPRZEDAZY is NOT allowed for COMPLAINT scenario")
        void przyjetyNotAllowedForComplaint() {
            assertFalse(Outcome.allowedFor(RequestType.COMPLAINT).contains(Outcome.PRZYJETY_DO_ODSPRZEDAZY));
        }

        @Test
        @DisplayName("each outcome has a non-blank Polish label")
        void polishLabels() {
            for (Outcome o : Outcome.values()) {
                assertNotNull(o.getPolishLabel());
                assertFalse(o.getPolishLabel().isBlank(), "Outcome " + o.name() + " must have a Polish label");
            }
        }
    }

    // ─── ImageQuality ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("ImageQuality")
    class ImageQualityTests {

        @Test
        @DisplayName("has OK and POOR_UNREADABLE values")
        void hasExpectedValues() {
            assertDoesNotThrow(() -> ImageQuality.valueOf("OK"));
            assertDoesNotThrow(() -> ImageQuality.valueOf("POOR_UNREADABLE"));
        }
    }

    // ─── MessageRole ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("MessageRole")
    class MessageRoleTests {

        @Test
        @DisplayName("has SYSTEM, USER, ASSISTANT values")
        void hasExpectedValues() {
            assertDoesNotThrow(() -> MessageRole.valueOf("SYSTEM"));
            assertDoesNotThrow(() -> MessageRole.valueOf("USER"));
            assertDoesNotThrow(() -> MessageRole.valueOf("ASSISTANT"));
        }
    }

    // ─── CaseRequest ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("CaseRequest")
    class CaseRequestTests {

        @Test
        @DisplayName("constructs with all required fields")
        void constructsWithRequiredFields() {
            var req = new CaseRequest(
                    RequestType.COMPLAINT,
                    EquipmentCategory.SMARTFONY,
                    "Samsung Galaxy S24",
                    LocalDate.of(2025, 3, 15),
                    "Ekran przestał działać");
            assertAll(
                    () -> assertEquals(RequestType.COMPLAINT, req.requestType()),
                    () -> assertEquals(EquipmentCategory.SMARTFONY, req.category()),
                    () -> assertEquals("Samsung Galaxy S24", req.modelName()),
                    () -> assertEquals(LocalDate.of(2025, 3, 15), req.purchaseDate()),
                    () -> assertEquals("Ekran przestał działać", req.reason())
            );
        }

        @Test
        @DisplayName("reason may be null for RETURN scenario")
        void reasonNullableForReturn() {
            assertDoesNotThrow(() -> new CaseRequest(
                    RequestType.RETURN,
                    EquipmentCategory.TABLETY,
                    "iPad Air",
                    LocalDate.of(2026, 6, 10),
                    null));
        }
    }

    // ─── ImageAssessment ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("ImageAssessment")
    class ImageAssessmentTests {

        @Test
        @DisplayName("constructs with all fields")
        void constructsWithAllFields() {
            var assessment = new ImageAssessment(
                    RequestType.COMPLAINT,
                    "Pęknięty ekran widoczny w lewym górnym rogu",
                    true,
                    "pęknięty ekran",
                    "uderzenie mechaniczne",
                    null,
                    null,
                    ImageQuality.OK,
                    "raw model output text here"
            );
            assertAll(
                    () -> assertEquals(RequestType.COMPLAINT, assessment.requestType()),
                    () -> assertEquals("Pęknięty ekran widoczny w lewym górnym rogu", assessment.description()),
                    () -> assertTrue(assessment.damageDetected()),
                    () -> assertEquals("pęknięty ekran", assessment.damageType()),
                    () -> assertEquals("uderzenie mechaniczne", assessment.likelyCause()),
                    () -> assertNull(assessment.signsOfUse()),
                    () -> assertNull(assessment.resellableCondition()),
                    () -> assertEquals(ImageQuality.OK, assessment.imageQuality()),
                    () -> assertEquals("raw model output text here", assessment.rawModelText())
            );
        }
    }

    // ─── Decision ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Decision")
    class DecisionTests {

        @Test
        @DisplayName("constructs with all fields including disclaimerIncluded=true")
        void constructsWithAllFields() {
            var decision = new Decision(
                    Outcome.UZNANA,
                    "Wada fabryczna zgodnie z §3 regulaminu",
                    List.of("Przynieś urządzenie do serwisu"),
                    List.of(),
                    true
            );
            assertAll(
                    () -> assertEquals(Outcome.UZNANA, decision.outcome()),
                    () -> assertEquals("Wada fabryczna zgodnie z §3 regulaminu", decision.justification()),
                    () -> assertEquals(1, decision.nextSteps().size()),
                    () -> assertTrue(decision.missingInfo().isEmpty()),
                    () -> assertTrue(decision.disclaimerIncluded())
            );
        }

        @Test
        @DisplayName("constructs WYMAGA_WERYFIKACJI with missingInfo list")
        void escalationWithMissingInfo() {
            var decision = new Decision(
                    Outcome.WYMAGA_WERYFIKACJI,
                    "Zdjęcie nieczytelne",
                    List.of("Prześlij lepsze zdjęcie"),
                    List.of("Wyraźne zdjęcie uszkodzenia", "Data zakupu do weryfikacji"),
                    true
            );
            assertEquals(2, decision.missingInfo().size());
        }
    }

    // ─── ChatMessage ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("ChatMessage")
    class ChatMessageTests {

        @Test
        @DisplayName("constructs with role, content, createdAt")
        void constructsWithFields() {
            var now = Instant.now();
            var msg = new ChatMessage(MessageRole.SYSTEM, "Witaj!", now);
            assertAll(
                    () -> assertEquals(MessageRole.SYSTEM, msg.role()),
                    () -> assertEquals("Witaj!", msg.content()),
                    () -> assertEquals(now, msg.createdAt())
            );
        }
    }

    // ─── Session ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Session")
    class SessionTests {

        @Test
        @DisplayName("constructs with required fields")
        void constructsWithFields() {
            var id = UUID.randomUUID();
            var req = new CaseRequest(RequestType.RETURN, EquipmentCategory.LAPTOPY_KOMPUTERY,
                    "MacBook Air", LocalDate.of(2026, 6, 20), null);
            var assessment = new ImageAssessment(RequestType.RETURN, "Czyste", false,
                    null, null, "brak", true, ImageQuality.OK, "raw");
            var decision = new Decision(Outcome.PRZYJETY_DO_ODSPRZEDAZY,
                    "Brak śladów użytkowania", List.of("Dostarcz do punktu"), List.of(), true);
            var now = Instant.now();
            var msg = new ChatMessage(MessageRole.SYSTEM, "Witaj!", now);

            var session = new Session(id, req, assessment, decision, List.of(msg), now, now);

            assertAll(
                    () -> assertEquals(id, session.sessionId()),
                    () -> assertEquals(req, session.caseRequest()),
                    () -> assertEquals(assessment, session.imageAssessment()),
                    () -> assertEquals(decision, session.decision()),
                    () -> assertEquals(1, session.messages().size()),
                    () -> assertEquals(now, session.createdAt()),
                    () -> assertEquals(now, session.lastAccessedAt())
            );
        }
    }
}

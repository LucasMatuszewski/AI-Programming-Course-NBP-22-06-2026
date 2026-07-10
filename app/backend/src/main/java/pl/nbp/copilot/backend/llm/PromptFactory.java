package pl.nbp.copilot.backend.llm;

import org.springframework.stereotype.Component;
import pl.nbp.copilot.backend.cases.*;

/**
 * Assembles LLM prompts for all three pipeline stages.
 *
 * <p>All text is in Polish — the product language. Prompts instruct the model
 * to return strict JSON where applicable so {@link DecisionParser} and the
 * image assessment parser can deserialize responses without fragile regex.
 */
@Component
public class PromptFactory {

    /**
     * A system+user prompt pair for the vision stage.
     *
     * @param systemText system message (unused by {@link OpenRouterLlmClient#analyzeImage}
     *                   which sends a single user turn; kept for interface completeness)
     * @param userText   the instruction text sent alongside the image
     */
    public record Prompt(String systemText, String userText) {}

    // ─── Stage 1: image analysis ───────────────────────────────────────────────

    /**
     * Returns the image analysis prompt for the given request scenario.
     *
     * <p>The user text instructs the vision model to return a JSON object
     * matching the {@code ImageAssessmentDto} structure.
     *
     * @param requestType COMPLAINT or RETURN
     * @return prompt pair; only {@code userText} is consumed by the current LLM client
     */
    public Prompt imagePrompt(RequestType requestType) {
        String systemText = "Jesteś ekspertem oceniającym stan sprzętu elektronicznego na podstawie zdjęć.";
        String userText = switch (requestType) {
            case COMPLAINT -> complaintImagePrompt();
            case RETURN -> returnImagePrompt();
        };
        return new Prompt(systemText, userText);
    }

    private String complaintImagePrompt() {
        return """
                Przeanalizuj poniższe zdjęcie sprzętu elektronicznego zgłoszonego do reklamacji.
                Odpowiedz WYŁĄCZNIE w formacie JSON (bez markdown, bez komentarzy):

                {
                  "requestType": "COMPLAINT",
                  "description": "<szczegółowy opis tego co widać na zdjęciu>",
                  "damageDetected": <true lub false>,
                  "damageType": "<rodzaj uszkodzenia, np. pęknięty ekran, ślady zalania — lub null>",
                  "likelyCause": "<prawdopodobna przyczyna, np. uderzenie mechaniczne, wada fabryczna — lub null>",
                  "signsOfUse": null,
                  "resellableCondition": null,
                  "imageQuality": "<OK lub POOR_UNREADABLE>"
                }

                Zasady:
                - imageQuality = "POOR_UNREADABLE" gdy zdjęcie jest nieczytelne, zbyt ciemne, rozmyte lub nie pokazuje stanu sprzętu
                - damageDetected = true gdy widać wyraźne uszkodzenie mechaniczne, zalanie lub inne wady
                - Wartości null dla pól których nie możesz ustalić
                """;
    }

    private String returnImagePrompt() {
        return """
                Przeanalizuj poniższe zdjęcie sprzętu elektronicznego zgłoszonego do zwrotu.
                Odpowiedz WYŁĄCZNIE w formacie JSON (bez markdown, bez komentarzy):

                {
                  "requestType": "RETURN",
                  "description": "<szczegółowy opis stanu towaru widocznego na zdjęciu>",
                  "damageDetected": <true lub false — czy widać uszkodzenia>,
                  "damageType": null,
                  "likelyCause": null,
                  "signsOfUse": "<opis śladów użytkowania lub 'brak widocznych śladów'>",
                  "resellableCondition": <true lub false — czy towar nadaje się do odsprzedaży>,
                  "imageQuality": "<OK lub POOR_UNREADABLE>"
                }

                Zasady:
                - imageQuality = "POOR_UNREADABLE" gdy zdjęcie jest nieczytelne, zbyt ciemne lub nie pozwala ocenić stanu
                - resellableCondition = true gdy towar wygląda jak nowy lub z minimalnymi śladami
                - resellableCondition = false gdy towar ma wyraźne ślady użytkowania lub uszkodzenia
                """;
    }

    // ─── Stage 2: reasoning / decision ────────────────────────────────────────

    /**
     * Builds the full reasoning prompt for the decision stage.
     *
     * <p>Combines case context, image assessment, and policy text into a single
     * prompt that asks the model to return a {@code DecisionDto}-shaped JSON.
     *
     * @param requestType request scenario
     * @param assessment  result of the vision stage
     * @param ctx         case metadata
     * @param policyText  loaded policy Markdown document
     * @return assembled reasoning prompt (sent as a single user message)
     */
    public String reasoningPrompt(
            RequestType requestType,
            ImageAssessment assessment,
            CaseContext ctx,
            String policyText
    ) {
        String allowedOutcomes = switch (requestType) {
            case COMPLAINT -> "UZNANA, ODRZUCONA, WYMAGA_WERYFIKACJI";
            case RETURN -> "PRZYJETY_DO_ODSPRZEDAZY, ODRZUCONA, WYMAGA_WERYFIKACJI";
        };

        return """
                Jesteś ekspertem ds. obsługi posprzedażowej sprzętu elektronicznego.
                Twoim zadaniem jest podjęcie wstępnej decyzji na podstawie regulaminu, danych zgłoszenia i analizy zdjęcia.

                ## Dane zgłoszenia
                - Typ: %s
                - Kategoria sprzętu: %s
                - Model: %s
                - Data zakupu: %s
                - Opis klienta: %s

                ## Ocena zdjęcia (analiza multimedialna)
                - Jakość zdjęcia: %s
                - Opis: %s
                - Wykryte uszkodzenie: %s
                - Rodzaj uszkodzenia: %s
                - Prawdopodobna przyczyna: %s
                - Ślady użytkowania: %s
                - Nadaje się do odsprzedaży: %s

                ## Regulamin
                %s

                ## Instrukcja
                Na podstawie powyższych informacji podejmij decyzję i odpowiedz WYŁĄCZNIE w formacie JSON (bez markdown):

                {
                  "outcome": "<jedna z wartości: %s>",
                  "justification": "<uzasadnienie powołujące się na regulamin i wyniki analizy zdjęcia, min. 2 zdania>",
                  "nextSteps": ["<krok 1>", "<krok 2>"],
                  "missingInfo": ["<brakujące informacje, wymagane gdy outcome=WYMAGA_WERYFIKACJI, puste gdy inne>"]
                }

                Zasady:
                - Uzasadnienie MUSI powołać się na konkretne punkty regulaminu
                - nextSteps to konkretne instrukcje dla klienta (co ma teraz zrobić)
                - missingInfo wypełnij tylko dla WYMAGA_WERYFIKACJI
                """.formatted(
                ctx.scenarioLabel(),
                ctx.categoryLabel(),
                ctx.modelName(),
                ctx.purchaseDateFormatted(),
                ctx.reason() != null ? ctx.reason() : "(brak opisu)",
                assessment.imageQuality(),
                assessment.description(),
                booleanPl(assessment.damageDetected()),
                nullPl(assessment.damageType()),
                nullPl(assessment.likelyCause()),
                nullPl(assessment.signsOfUse()),
                booleanPl(assessment.resellableCondition()),
                policyText,
                allowedOutcomes
        );
    }

    // ─── Stage 3: chat system prompt ──────────────────────────────────────────

    /**
     * Builds the system prompt for the chat continuation phase.
     *
     * <p>Gives the model full case context — the intake data, image assessment,
     * preliminary decision, and policy — so it can answer follow-up questions
     * accurately and consistently.
     *
     * @param ctx        case metadata
     * @param assessment vision-stage result
     * @param decision   reasoning-stage result
     * @param policyText loaded policy Markdown document
     * @return system prompt string
     */
    public String chatSystemPrompt(
            CaseContext ctx,
            ImageAssessment assessment,
            Decision decision,
            String policyText
    ) {
        return """
                Jesteś pomocnym asystentem AI wspierającym klienta w procesie obsługi posprzedażowej sprzętu elektronicznego NBP.
                Prowadzisz rozmowę po wydaniu wstępnej decyzji automatycznej. Odpowiadaj w języku polskim, profesjonalnie i empatycznie.

                ## Kontekst sprawy
                - Typ zgłoszenia: %s
                - Kategoria sprzętu: %s
                - Model: %s
                - Data zakupu: %s
                - Opis klienta: %s

                ## Wyniki analizy zdjęcia
                - Jakość zdjęcia: %s
                - Opis: %s
                - Wykryte uszkodzenie: %s

                ## Wstępna decyzja systemu
                - Wynik: %s
                - Uzasadnienie: %s
                - Kolejne kroki: %s

                ## Regulamin (źródło decyzji)
                %s

                ## Zasady prowadzenia rozmowy
                - Odpowiadaj wyłącznie po polsku
                - Bądź empatyczny i pomocny — klient może być niezadowolony
                - Wyjaśniaj decyzję odwołując się do konkretnych punktów regulaminu
                - Nie obiecuj zmiany decyzji — możesz zaproponować kontakt z pracownikiem
                - Przypominaj, że decyzja systemu jest wstępna i niewiążąca prawnie
                - Nie wykraczaj poza temat obsługi tej konkretnej sprawy
                """.formatted(
                ctx.scenarioLabel(),
                ctx.categoryLabel(),
                ctx.modelName(),
                ctx.purchaseDateFormatted(),
                ctx.reason() != null ? ctx.reason() : "(brak opisu)",
                assessment.imageQuality(),
                assessment.description(),
                booleanPl(assessment.damageDetected()),
                decision.outcome().getPolishLabel(),
                decision.justification(),
                String.join(", ", decision.nextSteps()),
                policyText
        );
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String booleanPl(Boolean value) {
        if (value == null) return "nieznane";
        return value ? "tak" : "nie";
    }

    private String nullPl(String value) {
        return value != null ? value : "(brak)";
    }
}

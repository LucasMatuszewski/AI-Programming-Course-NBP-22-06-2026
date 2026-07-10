package pl.nbp.copilot.backend.cases;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Builds a structured Polish Markdown message that summarises a {@link Decision}.
 *
 * <p>The message always contains five blocks in order:
 * <ol>
 *   <li>Greeting</li>
 *   <li>Decision status label (mapped from {@link Outcome})</li>
 *   <li>Justification</li>
 *   <li>Next steps (bulleted)</li>
 *   <li>Mandatory disclaimer (TAC-107 / AC-16)</li>
 * </ol>
 *
 * <p>When the outcome is {@link Outcome#WYMAGA_WERYFIKACJI}, an additional
 * "Brakujące informacje" section is rendered with the {@code missingInfo} items.
 */
@Component
public class DecisionMessageBuilder {

    /**
     * Exact disclaimer text required by TAC-107 / AC-16. Must not be altered.
     */
    static final String DISCLAIMER =
            "Ocena ma charakter wstępny i nie jest wiążąca prawnie; "
            + "ostateczne rozpatrzenie może wymagać weryfikacji przez pracownika.";

    private static final Map<Outcome, String> OUTCOME_LABELS;

    static {
        OUTCOME_LABELS = new EnumMap<>(Outcome.class);
        OUTCOME_LABELS.put(Outcome.UZNANA,                   "Uznana");
        OUTCOME_LABELS.put(Outcome.ODRZUCONA,                "Odrzucona");
        OUTCOME_LABELS.put(Outcome.WYMAGA_WERYFIKACJI,       "Wymaga weryfikacji");
        OUTCOME_LABELS.put(Outcome.PRZYJETY_DO_ODSPRZEDAZY,  "Przyjęty do odsprzedaży");
    }

    /**
     * Builds a Polish Markdown first-chat message for the given {@code decision}.
     *
     * @param decision the structured AI decision
     * @return a Markdown string ready to be delivered as the first assistant message
     */
    public String build(Decision decision) {
        var sb = new StringBuilder();

        // Block 1 — Greeting
        sb.append("Witaj!\n\n");

        // Block 2 — Decision status label
        String label = OUTCOME_LABELS.getOrDefault(decision.outcome(), decision.outcome().name());
        sb.append("## Decyzja: ").append(label).append("\n\n");

        // Block 3 — Justification
        sb.append("### Uzasadnienie\n\n");
        sb.append(decision.justification()).append("\n\n");

        // Block 4a — Missing info (only for WYMAGA_WERYFIKACJI and when list is non-empty)
        if (decision.outcome() == Outcome.WYMAGA_WERYFIKACJI
                && decision.missingInfo() != null
                && !decision.missingInfo().isEmpty()) {
            sb.append("### Brakujące informacje\n\n");
            for (String item : decision.missingInfo()) {
                sb.append("- ").append(item).append("\n");
            }
            sb.append("\n");
        }

        // Block 4 — Next steps
        if (decision.nextSteps() != null && !decision.nextSteps().isEmpty()) {
            sb.append("### Kolejne kroki\n\n");
            for (String step : decision.nextSteps()) {
                sb.append("- ").append(step).append("\n");
            }
            sb.append("\n");
        }

        // Block 5 — Mandatory disclaimer (TAC-107 / AC-16)
        sb.append("---\n\n");
        sb.append("> ").append(DISCLAIMER).append("\n");

        return sb.toString();
    }
}

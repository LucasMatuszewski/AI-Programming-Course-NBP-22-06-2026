package pl.nbp.copilot.backend.cases;

import java.util.List;

/**
 * Structured output of the reasoning agent (stage 2 of the AI pipeline).
 *
 * <p>{@code disclaimerIncluded} must always be {@code true} — the pipeline and
 * {@code DecisionMessageBuilder} enforce this invariant (AC-16 / TAC-107).
 *
 * @param outcome           decision outcome; must be valid for the request scenario
 * @param justification     explanation citing image assessment and at least one policy rule
 * @param nextSteps         concrete list of steps for the customer
 * @param missingInfo       items needed for verification; non-empty when outcome is
 *                          {@link Outcome#WYMAGA_WERYFIKACJI}
 * @param disclaimerIncluded must be {@code true}; the decision message always carries the
 *                           non-binding disclaimer
 */
public record Decision(
        Outcome outcome,
        String justification,
        List<String> nextSteps,
        List<String> missingInfo,
        boolean disclaimerIncluded
) {
}

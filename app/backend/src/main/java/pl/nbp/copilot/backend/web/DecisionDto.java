package pl.nbp.copilot.backend.web;

import pl.nbp.copilot.backend.cases.Decision;
import pl.nbp.copilot.backend.cases.Outcome;

import java.util.List;

/**
 * DTO for the {@link Decision} domain object — returned inside {@link CaseResultDto}.
 *
 * @param outcome       decision outcome
 * @param justification explanation citing image assessment and policy rule(s)
 * @param nextSteps     concrete steps for the customer
 * @param missingInfo   items needed for verification; non-empty when outcome is WYMAGA_WERYFIKACJI
 */
public record DecisionDto(
        Outcome outcome,
        String justification,
        List<String> nextSteps,
        List<String> missingInfo
) {

    /**
     * Converts a {@link Decision} domain object to a {@code DecisionDto}.
     *
     * @param decision the domain decision
     * @return a new {@code DecisionDto}
     */
    public static DecisionDto from(Decision decision) {
        return new DecisionDto(
                decision.outcome(),
                decision.justification(),
                decision.nextSteps(),
                decision.missingInfo()
        );
    }
}

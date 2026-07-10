package pl.nbp.copilot.backend.web;

import pl.nbp.copilot.backend.cases.CaseResult;
import pl.nbp.copilot.backend.cases.Outcome;

import java.util.UUID;

/**
 * Response DTO for {@code POST /api/cases} (HTTP 201).
 *
 * @param sessionId               unique identifier for the created session
 * @param outcome                 final decision outcome
 * @param decisionMessageMarkdown first ASSISTANT message in Polish Markdown
 * @param decision                structured decision details
 */
public record CaseResultDto(
        UUID sessionId,
        Outcome outcome,
        String decisionMessageMarkdown,
        DecisionDto decision
) {

    /**
     * Converts a {@link CaseResult} domain object to a {@code CaseResultDto}.
     *
     * @param result the domain result
     * @return a new {@code CaseResultDto}
     */
    public static CaseResultDto from(CaseResult result) {
        return new CaseResultDto(
                result.sessionId(),
                result.outcome(),
                result.decisionMessageMarkdown(),
                DecisionDto.from(result.decision())
        );
    }
}

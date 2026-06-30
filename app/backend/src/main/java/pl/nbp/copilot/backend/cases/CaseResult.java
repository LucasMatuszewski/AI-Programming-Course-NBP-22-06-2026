package pl.nbp.copilot.backend.cases;

import java.util.UUID;

/**
 * Outcome of the full case-creation pipeline (image validation → compression →
 * vision analysis → reasoning → decision → session persisted).
 *
 * @param sessionId               unique identifier of the created session
 * @param outcome                 final decision outcome
 * @param decisionMessageMarkdown first SYSTEM/ASSISTANT message in Polish Markdown
 * @param decision                full structured decision
 */
public record CaseResult(
        UUID sessionId,
        Outcome outcome,
        String decisionMessageMarkdown,
        Decision decision
) {
}

package pl.nbp.copilot.backend.cases;

/**
 * Structured output produced by the multimodal image analyzer (stage 1 of the AI pipeline).
 *
 * <p>Fields are nullable where they depend on the scenario:
 * {@code likelyCause} and {@code damageType} are meaningful for complaint;
 * {@code signsOfUse} and {@code resellableCondition} are meaningful for return.
 *
 * @param requestType          echoes the scenario that produced this assessment
 * @param description          free-text visual description of what the model observed
 * @param damageDetected       whether damage is visible ({@code null} = uncertain)
 * @param damageType           e.g. "pęknięty ekran", "ślady zalania" (complaint scenario)
 * @param likelyCause          e.g. "uderzenie mechaniczne", "wada fabryczna" (complaint)
 * @param signsOfUse           textual description of use signs (return scenario)
 * @param resellableCondition  whether the item appears resellable ({@code null} = uncertain)
 * @param imageQuality         OK or POOR_UNREADABLE — triggers escalation when unreadable
 * @param rawModelText         verbatim model output for traceability/debugging
 */
public record ImageAssessment(
        RequestType requestType,
        String description,
        Boolean damageDetected,
        String damageType,
        String likelyCause,
        String signsOfUse,
        Boolean resellableCondition,
        ImageQuality imageQuality,
        String rawModelText
) {
}

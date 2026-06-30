/**
 * Case orchestration layer: {@code CaseService} and {@code DecisionMessageBuilder}.
 *
 * <p>Responsibility: Orchestrates the full AI pipeline — validate → compress image →
 * analyze image (LLM vision) → reason (LLM structured) → build first decision message →
 * persist {@code Session}.
 *
 * <p>Note: Java reserves the identifier {@code case} as a keyword, so this package is
 * named {@code cases} (plural) instead. The ADR-001 logical name "case" maps to this package.
 *
 * <p>Depends on: {@code image}, {@code llm}, {@code policy}, {@code session}, {@code config}.
 * Depended on by: {@code web}.
 */
package pl.nbp.copilot.backend.cases;

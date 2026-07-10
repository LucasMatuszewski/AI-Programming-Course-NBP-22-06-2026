package pl.nbp.copilot.backend.cases;

import java.time.LocalDate;

/**
 * Immutable metadata snapshot of the customer's intake form submission.
 *
 * <p>Raw image bytes are NOT stored here — they are discarded after the pipeline
 * completes (per ADR-000 §8 and privacy requirements).
 *
 * @param requestType  complaint or return scenario
 * @param category     equipment category from the predefined list
 * @param modelName    free-text model/product name (non-blank)
 * @param purchaseDate date of purchase (must not be in the future)
 * @param reason       defect description — required for complaint, optional for return
 */
public record CaseRequest(
        RequestType requestType,
        EquipmentCategory category,
        String modelName,
        LocalDate purchaseDate,
        String reason
) {
}

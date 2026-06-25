package pl.nbp.copilot.backend.cases;

import java.util.EnumSet;
import java.util.Set;

/**
 * All possible decision outcomes across both request scenarios.
 *
 * <p>The complaint and return scenarios share {@code ODRZUCONA} and
 * {@code WYMAGA_WERYFIKACJI} but differ in the positive outcome:
 * <ul>
 *   <li>Complaint: {@code UZNANA} | {@code ODRZUCONA} | {@code WYMAGA_WERYFIKACJI}</li>
 *   <li>Return: {@code PRZYJETY_DO_ODSPRZEDAZY} | {@code ODRZUCONA} | {@code WYMAGA_WERYFIKACJI}</li>
 * </ul>
 *
 * <p>Use {@link #allowedFor(RequestType)} to obtain the scenario-correct set and enforce
 * that a decision value is valid for the current case.
 */
public enum Outcome {

    /** Complaint accepted — defect covered by policy (Uznana). */
    UZNANA("Uznana"),

    /** Case rejected — damage/return not covered by policy (Odrzucona). */
    ODRZUCONA("Odrzucona"),

    /** Requires manual verification — insufficient info or borderline case (Wymaga weryfikacji). */
    WYMAGA_WERYFIKACJI("Wymaga weryfikacji"),

    /** Return accepted for resale — item in resellable condition (Przyjęty do odsprzedaży). */
    PRZYJETY_DO_ODSPRZEDAZY("Przyjęty do odsprzedaży");

    private final String polishLabel;

    Outcome(String polishLabel) {
        this.polishLabel = polishLabel;
    }

    /**
     * Returns the Polish customer-facing label for this outcome.
     *
     * @return non-null, non-blank Polish label
     */
    public String getPolishLabel() {
        return polishLabel;
    }

    /**
     * Returns the set of outcomes that are valid for the given request type.
     *
     * @param requestType the scenario (complaint or return)
     * @return immutable set of valid outcomes for that scenario
     */
    public static Set<Outcome> allowedFor(RequestType requestType) {
        return switch (requestType) {
            case COMPLAINT -> EnumSet.of(UZNANA, ODRZUCONA, WYMAGA_WERYFIKACJI);
            case RETURN -> EnumSet.of(PRZYJETY_DO_ODSPRZEDAZY, ODRZUCONA, WYMAGA_WERYFIKACJI);
        };
    }
}

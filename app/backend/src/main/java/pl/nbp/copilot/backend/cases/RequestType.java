package pl.nbp.copilot.backend.cases;

/**
 * Distinguishes between a complaint (reklamacja) and a return (zwrot) scenario.
 *
 * <p>This drives prompt selection, policy document injection, and allowed outcome sets
 * throughout the pipeline.
 */
public enum RequestType {

    /** Reklamacja — customer reports a defect and requests repair/replacement/refund. */
    COMPLAINT,

    /** Zwrot — customer exercises the 14-day right of withdrawal from a distance sale. */
    RETURN
}

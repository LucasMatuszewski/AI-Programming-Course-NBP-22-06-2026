package pl.nbp.copilot.backend.cases;

/**
 * Image quality signal produced by the multimodal image analyzer.
 *
 * <p>When the analyzer determines the image is unreadable it sets this to
 * {@code POOR_UNREADABLE}, which the reasoning agent uses to escalate to
 * {@link Outcome#WYMAGA_WERYFIKACJI}.
 */
public enum ImageQuality {

    /** Image is clear enough to assess. */
    OK,

    /** Image is too dark, blurry, or otherwise unreadable; decision will require verification. */
    POOR_UNREADABLE
}

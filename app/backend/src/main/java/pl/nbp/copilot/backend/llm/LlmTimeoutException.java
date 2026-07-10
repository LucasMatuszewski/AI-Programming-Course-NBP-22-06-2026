package pl.nbp.copilot.backend.llm;

/**
 * Thrown when the upstream LLM call exceeds the configured timeout.
 *
 * <p>Maps to HTTP 504 Gateway Timeout at the controller boundary (ADR-003 §5).
 */
public class LlmTimeoutException extends RuntimeException {

    /**
     * Constructs a timeout exception with a descriptive message.
     *
     * @param message description of the timeout
     */
    public LlmTimeoutException(String message) {
        super(message);
    }

    /**
     * Constructs a timeout exception wrapping a root cause.
     *
     * @param message description of the timeout
     * @param cause   the underlying exception
     */
    public LlmTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}

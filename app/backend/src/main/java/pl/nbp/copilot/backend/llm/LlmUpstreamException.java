package pl.nbp.copilot.backend.llm;

/**
 * Thrown when the upstream LLM service returns a non-recoverable error (e.g. HTTP 4xx/5xx).
 *
 * <p>Maps to HTTP 502 Bad Gateway at the controller boundary (ADR-003 §5).
 */
public class LlmUpstreamException extends RuntimeException {

    /**
     * Constructs an upstream exception with a descriptive message.
     *
     * @param message description of the upstream error
     */
    public LlmUpstreamException(String message) {
        super(message);
    }

    /**
     * Constructs an upstream exception wrapping a root cause.
     *
     * @param message description of the upstream error
     * @param cause   the underlying exception
     */
    public LlmUpstreamException(String message, Throwable cause) {
        super(message, cause);
    }
}

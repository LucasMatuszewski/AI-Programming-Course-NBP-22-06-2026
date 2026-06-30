package pl.nbp.copilot.backend.policy;

/**
 * Thrown when a policy document cannot be loaded from the classpath — either because
 * the file is missing or because it is empty.
 *
 * <p>This is a fail-fast error that should not occur in a correctly deployed application.
 */
public class PolicyLoadException extends RuntimeException {

    /**
     * Creates the exception without a cause.
     *
     * @param message human-readable description
     */
    public PolicyLoadException(String message) {
        super(message);
    }

    /**
     * Creates the exception with a cause.
     *
     * @param message human-readable description
     * @param cause   underlying I/O exception
     */
    public PolicyLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}

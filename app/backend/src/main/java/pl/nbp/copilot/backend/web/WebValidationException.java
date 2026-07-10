package pl.nbp.copilot.backend.web;

import java.util.Map;

/**
 * Thrown by controllers for cross-field or semantic validation failures that can't be
 * expressed with Bean Validation annotations alone (e.g. "reason required for COMPLAINT").
 *
 * <p>Maps to HTTP 400 VALIDATION_ERROR at the web layer.
 */
public class WebValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    /**
     * Creates the exception with a field-error map.
     *
     * @param message     user-facing Polish message
     * @param fieldErrors map of field name → validation message
     */
    public WebValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors;
    }

    /**
     * Returns the field-level error details.
     *
     * @return non-null map of field name → message
     */
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}

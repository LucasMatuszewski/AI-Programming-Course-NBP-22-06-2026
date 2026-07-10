package pl.nbp.copilot.backend.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Standard error response body returned by {@code GlobalExceptionHandler}.
 *
 * <p>All {@code message} values are in Polish (user-facing).
 *
 * @param code        machine-readable error code (e.g. {@code VALIDATION_ERROR})
 * @param message     human-readable error description in Polish
 * @param fieldErrors map of field name → validation message; present only for 400 validation errors
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorDto(
        String code,
        String message,
        Map<String, String> fieldErrors
) {

    /**
     * Creates an error without field errors.
     *
     * @param code    machine-readable error code
     * @param message Polish-language message
     */
    public ErrorDto(String code, String message) {
        this(code, message, null);
    }
}

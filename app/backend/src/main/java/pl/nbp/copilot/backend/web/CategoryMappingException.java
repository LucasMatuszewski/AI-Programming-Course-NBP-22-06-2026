package pl.nbp.copilot.backend.web;

/**
 * Thrown when a frontend wire category value cannot be mapped to a known
 * {@link pl.nbp.copilot.backend.cases.EquipmentCategory} domain constant.
 *
 * <p>Maps to HTTP 400 VALIDATION_ERROR at the web layer.
 */
public class CategoryMappingException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message human-readable description (in Polish)
     */
    public CategoryMappingException(String message) {
        super(message);
    }
}

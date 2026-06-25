package pl.nbp.copilot.backend.image;

/**
 * Thrown when an uploaded image has a MIME type not in the allowed set
 * ({@code image/jpeg}, {@code image/png}, {@code image/webp}).
 *
 * <p>Maps to HTTP 415 Unsupported Media Type at the web layer.
 */
public class UnsupportedImageTypeException extends RuntimeException {

    private final String actualMimeType;

    /**
     * Creates the exception.
     *
     * @param actualMimeType the disallowed MIME type that was supplied
     * @param message        human-readable description
     */
    public UnsupportedImageTypeException(String actualMimeType, String message) {
        super(message);
        this.actualMimeType = actualMimeType;
    }

    /**
     * Returns the MIME type that was rejected.
     *
     * @return the actual (disallowed) MIME type
     */
    public String getActualMimeType() {
        return actualMimeType;
    }
}

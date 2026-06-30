package pl.nbp.copilot.backend.image;

/**
 * Thrown when image compression fails due to an unrecoverable error
 * (e.g. corrupted image data that Thumbnailator cannot decode).
 */
public class ImageCompressionException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message human-readable description
     * @param cause   the underlying exception
     */
    public ImageCompressionException(String message, Throwable cause) {
        super(message, cause);
    }
}

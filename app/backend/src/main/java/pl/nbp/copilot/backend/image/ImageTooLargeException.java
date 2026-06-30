package pl.nbp.copilot.backend.image;

/**
 * Thrown when an uploaded image exceeds the configured maximum byte size
 * ({@code APP_IMAGE_MAX_BYTES}, default 10 MB).
 *
 * <p>Maps to HTTP 413 Content Too Large at the web layer.
 */
public class ImageTooLargeException extends RuntimeException {

    private final long actualBytes;
    private final long maxBytes;

    /**
     * Creates the exception.
     *
     * @param actualBytes the size of the rejected image in bytes
     * @param maxBytes    the configured maximum in bytes
     * @param message     human-readable description
     */
    public ImageTooLargeException(long actualBytes, long maxBytes, String message) {
        super(message);
        this.actualBytes = actualBytes;
        this.maxBytes = maxBytes;
    }

    /** Returns the actual image size in bytes. */
    public long getActualBytes() {
        return actualBytes;
    }

    /** Returns the configured maximum in bytes. */
    public long getMaxBytes() {
        return maxBytes;
    }
}

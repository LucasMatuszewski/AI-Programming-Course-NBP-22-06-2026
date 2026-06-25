package pl.nbp.copilot.backend.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Validates an uploaded image against allowed MIME types and the configured size limit.
 *
 * <p>Allowed MIME types: {@code image/jpeg}, {@code image/png}, {@code image/webp}.
 *
 * <p>Instantiated as a Spring bean via
 * {@link pl.nbp.copilot.backend.config.ImageConfig}.
 *
 * <p>Throw semantics:
 * <ul>
 *   <li>{@link UnsupportedImageTypeException} — MIME type not in allowed set (→ HTTP 415)</li>
 *   <li>{@link ImageTooLargeException} — byte count exceeds {@code maxBytes} (→ HTTP 413)</li>
 *   <li>{@link IllegalArgumentException} — null or empty bytes array</li>
 * </ul>
 */
public class ImageValidator {

    private static final Logger log = LoggerFactory.getLogger(ImageValidator.class);

    /** Allowed MIME types per PRD §8 / AC-07. */
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final long maxBytes;

    /**
     * Creates a validator with the given maximum byte size.
     *
     * @param maxBytes maximum accepted image size in bytes (inclusive)
     */
    public ImageValidator(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    /**
     * Validates {@code imageBytes} against the allowed MIME types and the size limit.
     *
     * @param imageBytes  raw image bytes — must be non-null and non-empty
     * @param mimeType    declared MIME type of the upload
     * @param filename    original filename (used in log messages only)
     * @throws IllegalArgumentException       if {@code imageBytes} is null or empty
     * @throws UnsupportedImageTypeException  if {@code mimeType} is not in the allowed set
     * @throws ImageTooLargeException         if {@code imageBytes.length > maxBytes}
     */
    public void validate(byte[] imageBytes, String mimeType, String filename) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException(
                    "Obraz nie może być pusty. Plik: " + filename);
        }

        String normalizedMime = (mimeType == null) ? "" : mimeType.trim().toLowerCase();
        if (!ALLOWED_MIME_TYPES.contains(normalizedMime)) {
            log.debug("Rejected image '{}' with MIME type '{}'", filename, mimeType);
            throw new UnsupportedImageTypeException(mimeType,
                    "Nieobsługiwany typ pliku: '" + mimeType
                    + "'. Dozwolone formaty: JPG, PNG, WEBP.");
        }

        if (imageBytes.length > maxBytes) {
            log.debug("Rejected image '{}' with size {} bytes (max {})", filename, imageBytes.length, maxBytes);
            throw new ImageTooLargeException(imageBytes.length, maxBytes,
                    "Plik jest zbyt duży: " + imageBytes.length + " bajtów."
                    + " Maksymalny rozmiar to " + maxBytes + " bajtów (10 MB).");
        }
    }
}

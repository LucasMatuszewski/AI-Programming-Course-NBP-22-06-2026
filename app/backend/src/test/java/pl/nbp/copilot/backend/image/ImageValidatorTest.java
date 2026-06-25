package pl.nbp.copilot.backend.image;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD red-first tests for ImageValidator.
 */
@DisplayName("ImageValidator")
class ImageValidatorTest {

    private static final long MAX_BYTES = 10 * 1024 * 1024L; // 10 MB

    private ImageValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ImageValidator(MAX_BYTES);
    }

    // ─── MIME type checks ────────────────────────────────────────────────────

    @Nested
    @DisplayName("MIME type validation")
    class MimeTypeTests {

        @ParameterizedTest(name = "accepts {0}")
        @ValueSource(strings = {"image/jpeg", "image/png", "image/webp"})
        @DisplayName("accepts allowed MIME types without throwing")
        void acceptsAllowedMimeTypes(String mimeType) {
            byte[] tiny = new byte[1024];
            assertDoesNotThrow(() -> validator.validate(tiny, mimeType, "file.jpg"));
        }

        @ParameterizedTest(name = "rejects {0}")
        @ValueSource(strings = {"image/gif", "image/bmp", "image/tiff", "application/pdf", "text/plain"})
        @DisplayName("rejects disallowed MIME types with UnsupportedImageTypeException")
        void rejectsDisallowedMimeTypes(String mimeType) {
            byte[] tiny = new byte[1024];
            assertThrows(UnsupportedImageTypeException.class,
                    () -> validator.validate(tiny, mimeType, "file.gif"));
        }

        @Test
        @DisplayName("throws UnsupportedImageTypeException for null MIME type")
        void rejectsNullMimeType() {
            assertThrows(UnsupportedImageTypeException.class,
                    () -> validator.validate(new byte[100], null, "file.jpg"));
        }

        @Test
        @DisplayName("throws UnsupportedImageTypeException for blank MIME type")
        void rejectsBlankMimeType() {
            assertThrows(UnsupportedImageTypeException.class,
                    () -> validator.validate(new byte[100], "  ", "file.jpg"));
        }

        @Test
        @DisplayName("spoofed extension with GIF magic bytes detected by MIME, not filename")
        void mimeCheckBeatsFilenameExtension() {
            // File named .jpg but declared as image/gif — validator checks declared MIME
            byte[] tiny = new byte[1024];
            assertThrows(UnsupportedImageTypeException.class,
                    () -> validator.validate(tiny, "image/gif", "photo.jpg"));
        }
    }

    // ─── Size checks ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Size validation")
    class SizeTests {

        @Test
        @DisplayName("accepts image exactly at 10 MB limit")
        void acceptsExactlyMaxBytes() {
            byte[] exactMax = new byte[(int) MAX_BYTES];
            assertDoesNotThrow(() -> validator.validate(exactMax, "image/jpeg", "photo.jpg"));
        }

        @Test
        @DisplayName("rejects image one byte over 10 MB with ImageTooLargeException")
        void rejectsOneByteOverLimit() {
            byte[] overLimit = new byte[(int) MAX_BYTES + 1];
            assertThrows(ImageTooLargeException.class,
                    () -> validator.validate(overLimit, "image/jpeg", "photo.jpg"));
        }

        @Test
        @DisplayName("rejects null bytes array")
        void rejectsNullBytes() {
            assertThrows(IllegalArgumentException.class,
                    () -> validator.validate(null, "image/jpeg", "photo.jpg"));
        }

        @Test
        @DisplayName("rejects empty bytes array")
        void rejectsEmptyBytes() {
            assertThrows(IllegalArgumentException.class,
                    () -> validator.validate(new byte[0], "image/jpeg", "photo.jpg"));
        }
    }
}

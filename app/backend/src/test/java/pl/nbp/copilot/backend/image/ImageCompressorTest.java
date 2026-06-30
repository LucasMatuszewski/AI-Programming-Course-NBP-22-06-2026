package pl.nbp.copilot.backend.image;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for ImageCompressor.
 *
 * <p>Uses synthetic in-memory JPEG images so there is no filesystem dependency.
 */
@DisplayName("ImageCompressor")
class ImageCompressorTest {

    private ImageCompressor compressor;

    @BeforeEach
    void setUp() {
        compressor = new ImageCompressor();
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    /**
     * Creates a synthetic JPEG image of the given dimensions.
     */
    private byte[] createJpeg(int width, int height) throws Exception {
        var img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // Fill with a pattern so it's not a trivially empty image
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                img.setRGB(x, y, (x * 255 / width) << 16 | (y * 255 / height) << 8 | 128);
            }
        }
        var baos = new ByteArrayOutputStream();
        ImageIO.write(img, "JPEG", baos);
        return baos.toByteArray();
    }

    // ─── Tests ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("output bytes ≤ input bytes for a large image (AC-10 / TAC-103)")
    void outputBytesAtMostInputBytes_largeImage() throws Exception {
        byte[] input = createJpeg(3000, 2000);

        CompressedImage result = compressor.compress(input, "image/jpeg");

        assertTrue(result.bytes().length <= input.length,
                "Compressed output (" + result.bytes().length + " bytes) must be ≤ input ("
                + input.length + " bytes)");
    }

    @Test
    @DisplayName("does NOT upscale a small image (output bytes ≤ input bytes)")
    void doesNotUpscaleSmallImage() throws Exception {
        // A tiny 100x100 image — must not grow
        byte[] input = createJpeg(100, 100);

        CompressedImage result = compressor.compress(input, "image/jpeg");

        assertTrue(result.bytes().length <= input.length,
                "Small image must not be upscaled: output=" + result.bytes().length
                + " input=" + input.length);
    }

    @Test
    @DisplayName("returns valid base64 data URL with correct prefix")
    void returnsValidBase64DataUrl() throws Exception {
        byte[] input = createJpeg(800, 600);

        CompressedImage result = compressor.compress(input, "image/jpeg");

        assertNotNull(result.dataUrl());
        assertTrue(result.dataUrl().startsWith("data:image/"),
                "Data URL must start with 'data:image/'");
        assertTrue(result.dataUrl().contains(";base64,"),
                "Data URL must contain ';base64,'");

        // The base64 part must be decodable
        String b64Part = result.dataUrl().substring(result.dataUrl().indexOf(";base64,") + 8);
        assertDoesNotThrow(() -> Base64.getDecoder().decode(b64Part),
                "Base64 part of data URL must be decodable");
    }

    @Test
    @DisplayName("result bytes match the decoded base64 in the data URL")
    void bytesMatchDataUrl() throws Exception {
        byte[] input = createJpeg(1200, 900);

        CompressedImage result = compressor.compress(input, "image/jpeg");

        String b64 = result.dataUrl().substring(result.dataUrl().indexOf(";base64,") + 8);
        byte[] decodedFromUrl = Base64.getDecoder().decode(b64);
        assertArrayEquals(result.bytes(), decodedFromUrl,
                "Bytes in CompressedImage must match the decoded base64 in dataUrl");
    }

    @Test
    @DisplayName("output has a non-blank content type")
    void outputHasNonBlankContentType() throws Exception {
        byte[] input = createJpeg(500, 500);

        CompressedImage result = compressor.compress(input, "image/jpeg");

        assertNotNull(result.contentType());
        assertFalse(result.contentType().isBlank());
    }

    @Test
    @DisplayName("throws IllegalArgumentException for null input bytes")
    void throwsOnNullInput() {
        assertThrows(IllegalArgumentException.class,
                () -> compressor.compress(null, "image/jpeg"));
    }

    @Test
    @DisplayName("throws IllegalArgumentException for empty input bytes")
    void throwsOnEmptyInput() {
        assertThrows(IllegalArgumentException.class,
                () -> compressor.compress(new byte[0], "image/jpeg"));
    }
}

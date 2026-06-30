package pl.nbp.copilot.backend.image;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Compresses and optionally resizes an image before it is sent to the LLM vision model.
 *
 * <h3>Strategy</h3>
 * <ol>
 *   <li>Decode the input bytes with Thumbnailator.</li>
 *   <li>If either dimension exceeds {@code MAX_LONG_EDGE_PX} (1568 px), resize so the
 *       long edge equals {@code MAX_LONG_EDGE_PX} while preserving the aspect ratio.
 *       Never upscale a smaller image.</li>
 *   <li>Re-encode at {@code OUTPUT_QUALITY} (0.85). If the result is still larger than the
 *       input, try progressively lower qualities until it fits or we reach the floor.</li>
 *   <li>Return the compressed bytes, the output MIME type, and a base64 data URL.</li>
 * </ol>
 *
 * <p>Guarantees: output byte length ≤ input byte length (AC-10 / TAC-103).
 *
 * <p>Instantiated as a Spring bean via
 * {@link pl.nbp.copilot.backend.config.ImageConfig}.
 */
public class ImageCompressor {

    private static final Logger log = LoggerFactory.getLogger(ImageCompressor.class);

    /** Maximum long-edge dimension in pixels; matches the GPT-4o vision recommendation. */
    private static final int MAX_LONG_EDGE_PX = 1568;

    /** Initial re-encode quality (0.0–1.0). */
    private static final double OUTPUT_QUALITY = 0.85;

    /** Minimum quality floor to still attempt compression. */
    private static final double MIN_QUALITY = 0.50;

    /** Quality decrement per retry. */
    private static final double QUALITY_STEP = 0.10;

    /**
     * Compresses the supplied image bytes.
     *
     * @param inputBytes  raw image bytes — must be non-null and non-empty
     * @param mimeType    declared MIME type of the input (e.g. {@code image/jpeg})
     * @return {@link CompressedImage} with bytes ≤ input, content type, and data URL
     * @throws IllegalArgumentException if {@code inputBytes} is null or empty
     * @throws ImageCompressionException if compression fails
     */
    public CompressedImage compress(byte[] inputBytes, String mimeType) {
        if (inputBytes == null || inputBytes.length == 0) {
            throw new IllegalArgumentException("Input image bytes must not be null or empty");
        }

        try {
            return doCompress(inputBytes, mimeType);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new ImageCompressionException("Nie udało się skompresować obrazu: " + e.getMessage(), e);
        }
    }

    private CompressedImage doCompress(byte[] inputBytes, String mimeType) throws Exception {
        String outputFormat = resolveOutputFormat(mimeType);
        String outputMime = "image/" + outputFormat;

        double quality = OUTPUT_QUALITY;
        byte[] outputBytes;

        do {
            outputBytes = encodeWithQuality(inputBytes, outputFormat, quality);
            if (outputBytes.length <= inputBytes.length) {
                break;
            }
            quality -= QUALITY_STEP;
        } while (quality >= MIN_QUALITY);

        // Final safety net: if still larger, return the original bytes
        if (outputBytes.length > inputBytes.length) {
            log.debug("Compression did not reduce size; returning original bytes ({} → {} bytes)",
                    inputBytes.length, outputBytes.length);
            outputBytes = inputBytes;
            outputMime = (mimeType != null) ? mimeType : "image/jpeg";
        } else {
            log.debug("Compressed image {} → {} bytes (quality={})",
                    inputBytes.length, outputBytes.length, quality);
        }

        String dataUrl = buildDataUrl(outputMime, outputBytes);
        return new CompressedImage(outputBytes, outputMime, dataUrl);
    }

    private byte[] encodeWithQuality(byte[] inputBytes, String outputFormat, double quality) throws Exception {
        var in = new ByteArrayInputStream(inputBytes);
        var out = new ByteArrayOutputStream();

        Thumbnails.of(in)
                .size(MAX_LONG_EDGE_PX, MAX_LONG_EDGE_PX)
                .keepAspectRatio(true)
                .outputFormat(outputFormat)
                .outputQuality(quality)
                .toOutputStream(out);

        return out.toByteArray();
    }

    private String resolveOutputFormat(String mimeType) {
        if (mimeType == null) {
            return "jpeg";
        }
        return switch (mimeType.trim().toLowerCase()) {
            case "image/png" -> "png";
            case "image/webp" -> "jpeg"; // Thumbnailator doesn't write WebP; re-encode as JPEG
            default -> "jpeg";
        };
    }

    private String buildDataUrl(String mimeType, byte[] bytes) {
        String b64 = Base64.getEncoder().encodeToString(bytes);
        return "data:" + mimeType + ";base64," + b64;
    }
}

package pl.nbp.copilot.backend.image;

/**
 * Result of compressing/resizing an image.
 *
 * @param bytes       compressed image bytes (length ≤ original input length)
 * @param contentType MIME type of the output (e.g. {@code image/jpeg})
 * @param dataUrl     base64 data URL in the form {@code data:<contentType>;base64,<b64>}
 *                    ready to embed in the LLM vision prompt
 */
public record CompressedImage(
        byte[] bytes,
        String contentType,
        String dataUrl
) {
}

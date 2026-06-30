/**
 * Image processing layer: validation and compression/resize.
 *
 * <p>Responsibility:
 * <ul>
 *   <li>{@code ImageValidator} — MIME type check (jpeg/png/webp) and size check.</li>
 *   <li>{@code ImageCompressor} — Thumbnailator resize to max 1568px long edge + quality
 *       re-encode; guarantees output ≤ input bytes; produces a base64 data-URL.</li>
 * </ul>
 *
 * <p>Depends on: nothing (pure image-processing utilities).
 * Depended on by: {@code cases}.
 */
package pl.nbp.copilot.backend.image;

/**
 * Web layer: REST controllers ({@code CaseController}, {@code ChatController})
 * and {@code GlobalExceptionHandler}.
 *
 * <p>Responsibility: HTTP boundary — receives multipart case intake and SSE chat requests,
 * delegates all business logic to the {@code cases} package, returns DTOs or streams.
 *
 * <p>Dependency direction: {@code web} → {@code cases} → lower layers (no back-dependency on web).
 */
package pl.nbp.copilot.backend.web;

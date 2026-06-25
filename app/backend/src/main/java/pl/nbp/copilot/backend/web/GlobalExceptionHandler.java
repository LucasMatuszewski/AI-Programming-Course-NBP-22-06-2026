package pl.nbp.copilot.backend.web;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import pl.nbp.copilot.backend.image.ImageTooLargeException;
import pl.nbp.copilot.backend.image.UnsupportedImageTypeException;
import pl.nbp.copilot.backend.llm.LlmTimeoutException;
import pl.nbp.copilot.backend.llm.LlmUpstreamException;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps domain and infrastructure exceptions to standardised {@link ErrorDto} responses.
 *
 * <p>All {@code message} values are in Polish (user-facing).
 * Stack traces are never exposed to the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Bean-validation failures on request bodies ({@code @Valid} annotation).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity
                .badRequest()
                .body(new ErrorDto("VALIDATION_ERROR", "Dane wejściowe są nieprawidłowe.", fieldErrors));
    }

    /**
     * Bean-validation failures on method parameters ({@code @Validated} on class).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorDto> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getConstraintViolations().forEach(cv -> {
            String path = cv.getPropertyPath().toString();
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            fieldErrors.put(field, cv.getMessage());
        });
        return ResponseEntity
                .badRequest()
                .body(new ErrorDto("VALIDATION_ERROR", "Dane wejściowe są nieprawidłowe.", fieldErrors));
    }

    /**
     * Session not found or expired → 404 SESSION_NOT_FOUND.
     */
    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ErrorDto> handleSessionNotFound(SessionNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorDto("SESSION_NOT_FOUND", ex.getMessage()));
    }

    /**
     * Category wire-value unknown → 400 VALIDATION_ERROR.
     */
    @ExceptionHandler(CategoryMappingException.class)
    public ResponseEntity<ErrorDto> handleCategoryMapping(CategoryMappingException ex) {
        Map<String, String> fieldErrors = Map.of("category", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(new ErrorDto("VALIDATION_ERROR", ex.getMessage(), fieldErrors));
    }

    /**
     * Web-layer validation errors (e.g. from CaseController cross-field validation) → 400.
     */
    @ExceptionHandler(WebValidationException.class)
    public ResponseEntity<ErrorDto> handleWebValidation(WebValidationException ex) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorDto("VALIDATION_ERROR", ex.getMessage(), ex.getFieldErrors()));
    }

    /**
     * Unsupported image MIME type → 415.
     */
    @ExceptionHandler(UnsupportedImageTypeException.class)
    public ResponseEntity<ErrorDto> handleUnsupportedImageType(UnsupportedImageTypeException ex) {
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErrorDto("UNSUPPORTED_IMAGE_TYPE", ex.getMessage()));
    }

    /**
     * Image too large → 413.
     * Also handles Spring's {@link MaxUploadSizeExceededException} from multipart limits.
     */
    @ExceptionHandler(ImageTooLargeException.class)
    public ResponseEntity<ErrorDto> handleImageTooLarge(ImageTooLargeException ex) {
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorDto("IMAGE_TOO_LARGE", ex.getMessage()));
    }

    /**
     * Spring multipart size exceeded (before reaching the service layer) → 413.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorDto> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorDto("IMAGE_TOO_LARGE",
                        "Plik jest zbyt duży. Maksymalny rozmiar to 10 MB."));
    }

    /**
     * LLM upstream error → 502 Bad Gateway.
     */
    @ExceptionHandler(LlmUpstreamException.class)
    public ResponseEntity<ErrorDto> handleLlmUpstream(LlmUpstreamException ex) {
        log.warn("LLM upstream error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorDto("LLM_UPSTREAM_ERROR",
                        "Wystąpił błąd usługi AI. Spróbuj ponownie za chwilę."));
    }

    /**
     * LLM timeout → 504 Gateway Timeout.
     */
    @ExceptionHandler(LlmTimeoutException.class)
    public ResponseEntity<ErrorDto> handleLlmTimeout(LlmTimeoutException ex) {
        log.warn("LLM timeout: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.GATEWAY_TIMEOUT)
                .body(new ErrorDto("LLM_TIMEOUT",
                        "Upłynął limit czasu oczekiwania na odpowiedź AI. Spróbuj ponownie."));
    }

    /**
     * Catch-all for any unhandled exception → 500.
     * Never exposes stack trace to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleGeneral(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorDto("INTERNAL_ERROR",
                        "Wystąpił nieoczekiwany błąd serwera. Prosimy spróbować ponownie."));
    }
}

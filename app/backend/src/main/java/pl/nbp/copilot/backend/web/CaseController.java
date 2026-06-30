package pl.nbp.copilot.backend.web;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.nbp.copilot.backend.cases.CaseRequest;
import pl.nbp.copilot.backend.cases.CaseResult;
import pl.nbp.copilot.backend.cases.CaseService;
import pl.nbp.copilot.backend.cases.EquipmentCategory;
import pl.nbp.copilot.backend.cases.RequestType;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

/**
 * REST controller for case intake.
 *
 * <p>Endpoint: {@code POST /api/cases} — accepts a multipart form, validates it,
 * maps it to domain objects and delegates to {@link CaseService}.
 */
@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private static final Logger log = LoggerFactory.getLogger(CaseController.class);

    private final CaseService caseService;

    /**
     * Constructor injection.
     *
     * @param caseService orchestrates the case pipeline
     */
    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    /**
     * Accepts a new case submission as a multipart form.
     *
     * <p>Bean validation is applied on the form DTO. Additional cross-field
     * validation (e.g. {@code reason} required for COMPLAINT) is done here.
     *
     * @param form the bound and validated form fields
     * @return 201 Created with {@link CaseResultDto}
     * @throws WebValidationException      on cross-field validation failure
     * @throws CategoryMappingException    on unknown category wire value
     * @throws pl.nbp.copilot.backend.image.UnsupportedImageTypeException on bad MIME type
     * @throws pl.nbp.copilot.backend.image.ImageTooLargeException         on oversized image
     * @throws pl.nbp.copilot.backend.llm.LlmUpstreamException             on LLM upstream error
     * @throws pl.nbp.copilot.backend.llm.LlmTimeoutException              on LLM timeout
     */
    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public CaseResultDto createCase(@Valid @ModelAttribute CaseFormDto form) throws IOException {

        // --- Cross-field validation ---
        RequestType requestType = parseRequestType(form);
        LocalDate purchaseDate = parsePurchaseDate(form);
        EquipmentCategory category = form.toEquipmentCategory(); // throws CategoryMappingException on unknown

        if (requestType == RequestType.COMPLAINT
                && (form.getReason() == null || form.getReason().isBlank())) {
            throw new WebValidationException(
                    "Powód jest wymagany dla reklamacji.",
                    Map.of("reason", "Powód reklamacji jest wymagany."));
        }

        if (purchaseDate.isAfter(LocalDate.now())) {
            throw new WebValidationException(
                    "Data zakupu nie może być w przyszłości.",
                    Map.of("purchaseDate", "Data zakupu nie może być w przyszłości."));
        }

        // --- Read image bytes ---
        byte[] imageBytes = form.getImage().getBytes();
        String mimeType = form.getImage().getContentType();

        // --- Build domain request ---
        CaseRequest caseRequest = new CaseRequest(
                requestType,
                category,
                form.getModelName(),
                purchaseDate,
                form.getReason()
        );

        log.info("Creating case: requestType={}, category={}, model={}",
                requestType, category, form.getModelName());

        CaseResult result = caseService.createCase(caseRequest, imageBytes, mimeType);
        return CaseResultDto.from(result);
    }

    // --- Private helpers ---

    private RequestType parseRequestType(CaseFormDto form) {
        try {
            return RequestType.valueOf(form.getRequestType().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new WebValidationException(
                    "Nieznany typ zgłoszenia: '" + form.getRequestType() + "'",
                    Map.of("requestType", "Nieznany typ zgłoszenia: '" + form.getRequestType() + "'"));
        }
    }

    private LocalDate parsePurchaseDate(CaseFormDto form) {
        try {
            return LocalDate.parse(form.getPurchaseDate().trim());
        } catch (Exception e) {
            throw new WebValidationException(
                    "Nieprawidłowy format daty zakupu.",
                    Map.of("purchaseDate", "Wymagany format daty: YYYY-MM-DD"));
        }
    }
}

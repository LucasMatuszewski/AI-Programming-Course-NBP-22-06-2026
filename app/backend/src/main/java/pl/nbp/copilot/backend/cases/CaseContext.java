package pl.nbp.copilot.backend.cases;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Flattened, prompt-ready view of a {@link CaseRequest}.
 *
 * <p>Carries only the fields needed for prompt assembly, with pre-formatted strings
 * so {@link pl.nbp.copilot.backend.llm.PromptFactory} stays free of date-formatting logic.
 */
public record CaseContext(
        RequestType requestType,
        String categoryLabel,
        String modelName,
        LocalDate purchaseDate,
        String reason
) {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("pl"));

    public static CaseContext from(CaseRequest req) {
        return new CaseContext(
                req.requestType(),
                req.category().getDisplayLabel(),
                req.modelName(),
                req.purchaseDate(),
                req.reason()
        );
    }

    public String purchaseDateFormatted() {
        return purchaseDate != null ? purchaseDate.format(DATE_FMT) : "nieznana";
    }

    public String scenarioLabel() {
        return switch (requestType) {
            case COMPLAINT -> "Reklamacja";
            case RETURN -> "Zwrot";
        };
    }
}

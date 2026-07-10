package pl.nbp.copilot.backend.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.nbp.copilot.backend.cases.Decision;
import pl.nbp.copilot.backend.cases.Outcome;
import pl.nbp.copilot.backend.cases.RequestType;

import java.util.List;
import java.util.Set;

/**
 * Parses an LLM JSON response into a {@link Decision}.
 *
 * <p>Validates that the {@code outcome} field is in the correct enum set for the
 * scenario (complaint vs return). On any parse error, invalid enum value, or empty
 * input, returns a controlled {@link Outcome#WYMAGA_WERYFIKACJI} fallback with a
 * standard Polish message. Never throws to a 500 (TAC-304).
 */
@Component
public class DecisionParser {

    private static final Logger log = LoggerFactory.getLogger(DecisionParser.class);

    private static final String FALLBACK_MISSING_INFO = "Nie udało się jednoznacznie ocenić zgłoszenia";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Parses the raw LLM response text into a {@link Decision}.
     *
     * <p>Returns a {@link Outcome#WYMAGA_WERYFIKACJI} fallback on:
     * <ul>
     *   <li>null or blank input</li>
     *   <li>non-JSON or malformed JSON</li>
     *   <li>missing or unrecognized outcome value</li>
     *   <li>outcome value not allowed for the given scenario</li>
     * </ul>
     *
     * @param rawJson     the raw text from the LLM (may be null, blank, or malformed)
     * @param requestType the scenario (COMPLAINT or RETURN) used to validate the outcome set
     * @return a parsed {@link Decision}, or the fallback if parsing fails
     */
    public Decision parse(String rawJson, RequestType requestType) {
        if (rawJson == null || rawJson.isBlank()) {
            log.warn("DecisionParser: received null/blank LLM response; returning fallback");
            return fallback();
        }

        try {
            DecisionDto dto = OBJECT_MAPPER.readValue(rawJson, DecisionDto.class);

            if (dto.outcome == null || dto.outcome.isBlank()) {
                log.warn("DecisionParser: outcome field missing or blank; returning fallback");
                return fallback();
            }

            Outcome outcome;
            try {
                outcome = Outcome.valueOf(dto.outcome.trim());
            } catch (IllegalArgumentException e) {
                log.warn("DecisionParser: unrecognized outcome value '{}'; returning fallback", dto.outcome);
                return fallback();
            }

            Set<Outcome> allowed = Outcome.allowedFor(requestType);
            if (!allowed.contains(outcome)) {
                log.warn("DecisionParser: outcome '{}' is not allowed for scenario {}; returning fallback",
                        outcome, requestType);
                return fallback();
            }

            List<String> nextSteps = dto.nextSteps != null ? dto.nextSteps : List.of();
            List<String> missingInfo = dto.missingInfo != null ? dto.missingInfo : List.of();

            return new Decision(
                    outcome,
                    dto.justification != null ? dto.justification : "",
                    nextSteps,
                    missingInfo,
                    true
            );

        } catch (JsonProcessingException e) {
            log.warn("DecisionParser: failed to parse LLM JSON response: {}; returning fallback", e.getMessage());
            return fallback();
        } catch (Exception e) {
            log.error("DecisionParser: unexpected error during parse; returning fallback", e);
            return fallback();
        }
    }

    /**
     * Builds the standard {@link Outcome#WYMAGA_WERYFIKACJI} fallback decision.
     *
     * @return the fallback decision
     */
    private Decision fallback() {
        return new Decision(
                Outcome.WYMAGA_WERYFIKACJI,
                "Ocena automatyczna nie powiodła się. Zgłoszenie wymaga weryfikacji przez pracownika.",
                List.of(),
                List.of(FALLBACK_MISSING_INFO),
                true
        );
    }

    /**
     * Internal DTO for deserializing the LLM JSON output.
     * Extra fields are silently ignored ({@code @JsonIgnoreProperties}).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DecisionDto {

        public String outcome;
        public String justification;
        public List<String> nextSteps;
        public List<String> missingInfo;
    }
}

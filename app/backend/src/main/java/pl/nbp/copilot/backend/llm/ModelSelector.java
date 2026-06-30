package pl.nbp.copilot.backend.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Selects the appropriate model identifier for each LLM call stage.
 *
 * <p>Values are bound from environment variables / application.yml:
 * <ul>
 *   <li>{@code OPENROUTER_VISION_MODEL} → {@code openrouter.vision-model}</li>
 *   <li>{@code OPENROUTER_TEXT_MODEL}   → {@code openrouter.text-model}</li>
 * </ul>
 *
 * <p>Model IDs are never hardcoded — they are configuration values only (ADR-003).
 */
@Component
public class ModelSelector {

    private final String visionModel;
    private final String textModel;

    /**
     * Creates a ModelSelector with the configured model identifiers.
     *
     * @param visionModel model ID for the image analysis (vision) stage
     * @param textModel   model ID for the reasoning and chat stages
     */
    public ModelSelector(
            @Value("${openrouter.vision-model}") String visionModel,
            @Value("${openrouter.text-model}") String textModel
    ) {
        this.visionModel = visionModel;
        this.textModel = textModel;
    }

    /**
     * Returns the vision model identifier for image analysis calls.
     *
     * @return non-null model ID string
     */
    public String visionModel() {
        return visionModel;
    }

    /**
     * Returns the text model identifier for reasoning and chat calls.
     *
     * @return non-null model ID string
     */
    public String textModel() {
        return textModel;
    }
}

package pl.nbp.copilot.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the Hardware Service Decision Copilot backend.
 *
 * <p>Technology stack as defined by the project ADRs:
 * <ul>
 *   <li>Spring Boot 3.5.x (web, validation, actuator)</li>
 *   <li>com.openai:openai-java → OpenRouter (Chat Completions)</li>
 *   <li>Thumbnailator for server-side image compression</li>
 * </ul>
 *
 * @see pl.nbp.copilot.backend.config.AppProperties
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}

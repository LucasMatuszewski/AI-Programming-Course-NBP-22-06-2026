package pl.nbp.copilot.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.nbp.copilot.backend.image.ImageCompressor;
import pl.nbp.copilot.backend.image.ImageValidator;

/**
 * Spring configuration for image-processing beans.
 *
 * <p>Wires the {@link ImageValidator} with the {@code app.image.max-bytes} property
 * from {@link AppProperties}.
 */
@Configuration
public class ImageConfig {

    private final AppProperties appProperties;

    /**
     * Creates the config with the application properties.
     *
     * @param appProperties typed configuration bean
     */
    public ImageConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * Creates the {@link ImageValidator} bean with the configured max image size.
     *
     * @return configured {@link ImageValidator}
     */
    @Bean
    public ImageValidator imageValidator() {
        return new ImageValidator(appProperties.getImage().getMaxBytes());
    }

    /**
     * Creates the {@link ImageCompressor} bean.
     *
     * @return {@link ImageCompressor}
     */
    @Bean
    public ImageCompressor imageCompressor() {
        return new ImageCompressor();
    }
}

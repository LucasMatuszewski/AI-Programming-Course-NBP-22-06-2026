package pl.nbp.copilot.backend.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.nbp.copilot.backend.cases.RequestType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * Loads and caches the policy Markdown documents from the classpath.
 *
 * <p>Files are read once from {@code classpath:policies/} and cached in memory
 * for the lifetime of the bean. A missing or empty file causes a fail-fast
 * {@link PolicyLoadException}.
 *
 * <p>Policy documents per request type (per ADR-000 §8):
 * <ul>
 *   <li>{@link RequestType#COMPLAINT} → {@code policies/complaint-policy.md}</li>
 *   <li>{@link RequestType#RETURN} → {@code policies/return-policy.md}</li>
 * </ul>
 */
@Component
public class PolicyProvider {

    private static final Logger log = LoggerFactory.getLogger(PolicyProvider.class);

    private static final Map<RequestType, String> CLASSPATH_PATHS = Map.of(
            RequestType.COMPLAINT, "policies/complaint-policy.md",
            RequestType.RETURN, "policies/return-policy.md"
    );

    private final Map<RequestType, String> cache = new EnumMap<>(RequestType.class);

    /**
     * Loads the policy document for the given request type.
     *
     * <p>First call reads from the classpath and caches the result; subsequent calls
     * return the cached string.
     *
     * @param requestType the scenario whose policy document to load
     * @return non-null, non-blank Markdown policy text
     * @throws PolicyLoadException if the policy file is missing or empty
     */
    public synchronized String load(RequestType requestType) {
        return cache.computeIfAbsent(requestType, this::readFromClasspath);
    }

    private String readFromClasspath(RequestType requestType) {
        String path = CLASSPATH_PATHS.get(requestType);
        log.debug("Loading policy for {} from classpath:{}", requestType, path);

        InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
        if (stream == null) {
            throw new PolicyLoadException(
                    "Nie znaleziono dokumentu polityki na ścieżce: classpath:" + path);
        }

        try (stream) {
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            if (content.isBlank()) {
                throw new PolicyLoadException(
                        "Dokument polityki jest pusty: classpath:" + path);
            }
            log.debug("Loaded policy for {} ({} chars)", requestType, content.length());
            return content;
        } catch (IOException e) {
            throw new PolicyLoadException(
                    "Błąd odczytu dokumentu polityki: classpath:" + path, e);
        }
    }
}

package pl.nbp.copilot.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that both policy Markdown files are present on the classpath under
 * {@code policies/} and are non-empty.
 *
 * <p>These files are the build-time mirror of {@code docs/policies/} and are
 * loaded at runtime by {@code PolicyProvider} to supply policy text to the LLM.
 *
 * <p>This is a pure unit-level test — no Spring context required.
 */
@DisplayName("Policy files on classpath")
class PolicyFilesTest {

    @Test
    @DisplayName("complaint-policy.md is present and non-empty")
    void complaintPolicyIsPresent() throws Exception {
        assertPolicyFilePresent("policies/complaint-policy.md");
    }

    @Test
    @DisplayName("return-policy.md is present and non-empty")
    void returnPolicyIsPresent() throws Exception {
        assertPolicyFilePresent("policies/return-policy.md");
    }

    private void assertPolicyFilePresent(String classpathPath) throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(classpathPath);
        assertNotNull(stream, "Expected classpath resource to exist: " + classpathPath);

        byte[] bytes = stream.readAllBytes();
        assertTrue(bytes.length > 0,
                "Expected policy file to be non-empty: " + classpathPath);
    }
}

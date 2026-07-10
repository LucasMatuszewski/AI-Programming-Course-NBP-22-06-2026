package pl.nbp.copilot.backend.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.nbp.copilot.backend.cases.RequestType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for PolicyProvider.
 *
 * <p>No Spring context needed — PolicyProvider only reads classpath resources.
 */
@DisplayName("PolicyProvider")
class PolicyProviderTest {

    @Test
    @DisplayName("loads non-empty complaint policy text for COMPLAINT request type")
    void loadsComplaintPolicy() {
        PolicyProvider provider = new PolicyProvider();

        String policy = provider.load(RequestType.COMPLAINT);

        assertNotNull(policy, "Policy text must not be null");
        assertFalse(policy.isBlank(), "Policy text must not be blank");
        assertTrue(policy.contains("reklamacj"), // Polish keyword present in complaint-policy.md
                "Complaint policy must contain 'reklamacj'");
    }

    @Test
    @DisplayName("loads non-empty return policy text for RETURN request type")
    void loadsReturnPolicy() {
        PolicyProvider provider = new PolicyProvider();

        String policy = provider.load(RequestType.RETURN);

        assertNotNull(policy, "Policy text must not be null");
        assertFalse(policy.isBlank(), "Policy text must not be blank");
        assertTrue(policy.contains("zwrot"), // Polish keyword present in return-policy.md
                "Return policy must contain 'zwrot'");
    }

    @Test
    @DisplayName("complaint and return policies are distinct documents")
    void policiesAreDistinct() {
        PolicyProvider provider = new PolicyProvider();

        String complaint = provider.load(RequestType.COMPLAINT);
        String returnPolicy = provider.load(RequestType.RETURN);

        assertNotEquals(complaint, returnPolicy,
                "Complaint and return policies must be different documents");
    }

    @Test
    @DisplayName("caches policy — second call returns same content")
    void cacheReturnsConsistentContent() {
        PolicyProvider provider = new PolicyProvider();

        String first = provider.load(RequestType.COMPLAINT);
        String second = provider.load(RequestType.COMPLAINT);

        assertEquals(first, second, "Cached policy must equal first load");
    }
}

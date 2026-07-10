package pl.nbp.copilot.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration smoke tests: context loads, actuator health returns 200 UP.
 *
 * <p>Uses {@code RANDOM_PORT}-equivalent via {@code @SpringBootTest} + {@code @AutoConfigureMockMvc}
 * so it exercises the real filter chain without a real TCP port.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("BackendApplication integration smoke tests")
class BackendApplicationIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Spring context loads without errors")
    void contextLoads() {
        // If the context fails to load, this test fails with an exception —
        // that is the intended signal.
    }

    @Test
    @DisplayName("GET /actuator/health returns HTTP 200 with status UP")
    void actuatorHealthReturns200Up() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}

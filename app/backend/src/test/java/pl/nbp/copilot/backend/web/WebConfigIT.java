package pl.nbp.copilot.backend.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.nbp.copilot.backend.llm.LlmClient;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for WebConfig: CORS and context loading.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("WebConfig — CORS and context loading")
class WebConfigIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LlmClient llmClient;

    @Test
    @DisplayName("CORS preflight OPTIONS /api/cases with allowed origin → 200 with ACAO header")
    void corsPreflightAllowedOrigin_returns200WithAcaoHeader() throws Exception {
        mockMvc.perform(options("/api/cases")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("Spring application context loads with only required env vars")
    void contextLoads() {
        // If context doesn't load, this test fails automatically
    }
}

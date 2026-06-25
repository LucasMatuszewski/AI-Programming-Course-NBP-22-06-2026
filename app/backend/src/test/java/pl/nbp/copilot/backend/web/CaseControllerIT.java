package pl.nbp.copilot.backend.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import pl.nbp.copilot.backend.cases.Decision;
import pl.nbp.copilot.backend.cases.ImageAssessment;
import pl.nbp.copilot.backend.cases.ImageQuality;
import pl.nbp.copilot.backend.cases.Outcome;
import pl.nbp.copilot.backend.cases.RequestType;
import pl.nbp.copilot.backend.llm.LlmClient;
import pl.nbp.copilot.backend.llm.LlmTimeoutException;
import pl.nbp.copilot.backend.llm.LlmUpstreamException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for POST /api/cases.
 * LLM is mocked via {@code @MockBean}; all other beans run for real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("CaseController — POST /api/cases")
class CaseControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LlmClient llmClient;

    private static final byte[] FAKE_JPEG = createRealJpeg();

    /** Returns a real 10x10 JPEG for testing (passes ImageCompressor). */
    private static byte[] createRealJpeg() {
        try {
            BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 10; x++) {
                for (int y = 0; y < 10; y++) {
                    img.setRGB(x, y, 0xFF5544);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "jpeg", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test JPEG", e);
        }
    }

    private ImageAssessment okComplaintAssessment() {
        return new ImageAssessment(RequestType.COMPLAINT, "Pęknięty ekran", true, "Pęknięty ekran",
                "Uderzenie", null, null, ImageQuality.OK, "raw");
    }

    private ImageAssessment okReturnAssessment() {
        return new ImageAssessment(RequestType.RETURN, "Stan dobry", false, null,
                null, "Minimalne ślady", true, ImageQuality.OK, "raw");
    }

    private Decision complaintDecision() {
        return new Decision(Outcome.UZNANA, "Uzasadnienie reklamacji",
                List.of("Krok 1"), List.of(), true);
    }

    private Decision returnDecision() {
        return new Decision(Outcome.PRZYJETY_DO_ODSPRZEDAZY, "Zwrot przyjęty",
                List.of("Krok 1"), List.of(), true);
    }

    @BeforeEach
    void resetMocks() {
        reset(llmClient);
    }

    @Test
    @DisplayName("valid COMPLAINT multipart → 201 CaseResultDto with sessionId")
    void postCase_validComplaint_returns201() throws Exception {
        when(llmClient.analyzeImage(anyString(), anyString(), anyString()))
                .thenReturn(okComplaintAssessment());
        when(llmClient.decide(anyString(), eq(RequestType.COMPLAINT)))
                .thenReturn(complaintDecision());

        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, FAKE_JPEG);

        mockMvc.perform(multipart("/api/cases")
                        .file(image)
                        .param("requestType", "COMPLAINT")
                        .param("category", "SMARTPHONES")
                        .param("modelName", "Samsung Galaxy S24")
                        .param("purchaseDate", "2024-01-15")
                        .param("reason", "Pęknięty ekran"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.outcome").value("UZNANA"))
                .andExpect(jsonPath("$.decisionMessageMarkdown").isNotEmpty())
                .andExpect(jsonPath("$.decision.outcome").value("UZNANA"))
                .andExpect(jsonPath("$.decision.justification").isNotEmpty());
    }

    @Test
    @DisplayName("valid RETURN multipart → 201 CaseResultDto")
    void postCase_validReturn_returns201() throws Exception {
        when(llmClient.analyzeImage(anyString(), anyString(), anyString()))
                .thenReturn(okReturnAssessment());
        when(llmClient.decide(anyString(), eq(RequestType.RETURN)))
                .thenReturn(returnDecision());

        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, FAKE_JPEG);

        mockMvc.perform(multipart("/api/cases")
                        .file(image)
                        .param("requestType", "RETURN")
                        .param("category", "LAPTOPS")
                        .param("modelName", "Dell XPS 15")
                        .param("purchaseDate", "2024-06-01"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.outcome").value("PRZYJETY_DO_ODSPRZEDAZY"));
    }

    @Test
    @DisplayName("missing reason for COMPLAINT → 400 VALIDATION_ERROR with fieldErrors.reason")
    void postCase_missingReasonForComplaint_returns400() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, FAKE_JPEG);

        mockMvc.perform(multipart("/api/cases")
                        .file(image)
                        .param("requestType", "COMPLAINT")
                        .param("category", "SMARTPHONES")
                        .param("modelName", "Samsung")
                        .param("purchaseDate", "2024-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.reason").exists());

        verifyNoInteractions(llmClient);
    }

    @Test
    @DisplayName("future purchaseDate → 400 VALIDATION_ERROR")
    void postCase_futurePurchaseDate_returns400() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, FAKE_JPEG);

        mockMvc.perform(multipart("/api/cases")
                        .file(image)
                        .param("requestType", "COMPLAINT")
                        .param("category", "SMARTPHONES")
                        .param("modelName", "Samsung")
                        .param("purchaseDate", "2099-01-01")
                        .param("reason", "Usterka"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(llmClient);
    }

    @Test
    @DisplayName("GIF image → 415 UNSUPPORTED_IMAGE_TYPE")
    void postCase_gifImage_returns415() throws Exception {
        MockMultipartFile gifImage = new MockMultipartFile(
                "image", "photo.gif", "image/gif", FAKE_JPEG);

        mockMvc.perform(multipart("/api/cases")
                        .file(gifImage)
                        .param("requestType", "COMPLAINT")
                        .param("category", "SMARTPHONES")
                        .param("modelName", "Samsung")
                        .param("purchaseDate", "2024-01-01")
                        .param("reason", "Usterka"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_IMAGE_TYPE"));

        verifyNoInteractions(llmClient);
    }

    @Test
    @DisplayName("image > 10 MB → 413 IMAGE_TOO_LARGE")
    void postCase_oversizedImage_returns413() throws Exception {
        byte[] bigImage = new byte[11 * 1024 * 1024]; // 11 MB
        MockMultipartFile bigFile = new MockMultipartFile(
                "image", "big.jpg", MediaType.IMAGE_JPEG_VALUE, bigImage);

        mockMvc.perform(multipart("/api/cases")
                        .file(bigFile)
                        .param("requestType", "COMPLAINT")
                        .param("category", "SMARTPHONES")
                        .param("modelName", "Samsung")
                        .param("purchaseDate", "2024-01-01")
                        .param("reason", "Usterka"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("IMAGE_TOO_LARGE"));

        verifyNoInteractions(llmClient);
    }

    @Test
    @DisplayName("LlmUpstreamException → 502 LLM_UPSTREAM_ERROR")
    void postCase_llmUpstreamException_returns502() throws Exception {
        when(llmClient.analyzeImage(anyString(), anyString(), anyString()))
                .thenThrow(new LlmUpstreamException("upstream error"));

        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, FAKE_JPEG);

        mockMvc.perform(multipart("/api/cases")
                        .file(image)
                        .param("requestType", "COMPLAINT")
                        .param("category", "SMARTPHONES")
                        .param("modelName", "Samsung")
                        .param("purchaseDate", "2024-01-01")
                        .param("reason", "Usterka"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("LLM_UPSTREAM_ERROR"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("LlmTimeoutException → 504 LLM_TIMEOUT")
    void postCase_llmTimeoutException_returns504() throws Exception {
        when(llmClient.analyzeImage(anyString(), anyString(), anyString()))
                .thenThrow(new LlmTimeoutException("timeout"));

        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, FAKE_JPEG);

        mockMvc.perform(multipart("/api/cases")
                        .file(image)
                        .param("requestType", "COMPLAINT")
                        .param("category", "SMARTPHONES")
                        .param("modelName", "Samsung")
                        .param("purchaseDate", "2024-01-01")
                        .param("reason", "Usterka"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value("LLM_TIMEOUT"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("SMARTPHONES category wire value maps to SMARTFONY correctly → 201")
    void postCase_smartphonesCategoryMapping_returns201() throws Exception {
        when(llmClient.analyzeImage(anyString(), anyString(), anyString()))
                .thenReturn(okComplaintAssessment());
        when(llmClient.decide(anyString(), any()))
                .thenReturn(complaintDecision());

        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, FAKE_JPEG);

        mockMvc.perform(multipart("/api/cases")
                        .file(image)
                        .param("requestType", "COMPLAINT")
                        .param("category", "SMARTPHONES")
                        .param("modelName", "Test")
                        .param("purchaseDate", "2024-01-01")
                        .param("reason", "Usterka"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("AUDIO category wire value maps correctly → 201")
    void postCase_audioCategoryMapping_returns201() throws Exception {
        when(llmClient.analyzeImage(anyString(), anyString(), anyString()))
                .thenReturn(okComplaintAssessment());
        when(llmClient.decide(anyString(), any()))
                .thenReturn(complaintDecision());

        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, FAKE_JPEG);

        mockMvc.perform(multipart("/api/cases")
                        .file(image)
                        .param("requestType", "COMPLAINT")
                        .param("category", "AUDIO")
                        .param("modelName", "Test")
                        .param("purchaseDate", "2024-01-01")
                        .param("reason", "Usterka"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("OTHER category wire value maps correctly → 201")
    void postCase_otherCategoryMapping_returns201() throws Exception {
        when(llmClient.analyzeImage(anyString(), anyString(), anyString()))
                .thenReturn(okComplaintAssessment());
        when(llmClient.decide(anyString(), any()))
                .thenReturn(complaintDecision());

        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, FAKE_JPEG);

        mockMvc.perform(multipart("/api/cases")
                        .file(image)
                        .param("requestType", "COMPLAINT")
                        .param("category", "OTHER")
                        .param("modelName", "Test")
                        .param("purchaseDate", "2024-01-01")
                        .param("reason", "Usterka"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("unknown category wire value → 400 VALIDATION_ERROR")
    void postCase_unknownCategory_returns400() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, FAKE_JPEG);

        mockMvc.perform(multipart("/api/cases")
                        .file(image)
                        .param("requestType", "COMPLAINT")
                        .param("category", "UNKNOWN_CATEGORY")
                        .param("modelName", "Test")
                        .param("purchaseDate", "2024-01-01")
                        .param("reason", "Usterka"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(llmClient);
    }
}

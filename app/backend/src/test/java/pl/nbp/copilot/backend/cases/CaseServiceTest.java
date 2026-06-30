package pl.nbp.copilot.backend.cases;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.nbp.copilot.backend.image.CompressedImage;
import pl.nbp.copilot.backend.image.ImageCompressor;
import pl.nbp.copilot.backend.image.ImageValidator;
import pl.nbp.copilot.backend.image.UnsupportedImageTypeException;
import pl.nbp.copilot.backend.llm.LlmClient;
import pl.nbp.copilot.backend.llm.PromptFactory;
import pl.nbp.copilot.backend.policy.PolicyProvider;
import pl.nbp.copilot.backend.session.SessionStore;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CaseService — pipeline orchestration")
class CaseServiceTest {

    @Mock
    private ImageValidator imageValidator;
    @Mock
    private ImageCompressor imageCompressor;
    @Mock
    private LlmClient llmClient;
    @Mock
    private PolicyProvider policyProvider;
    @Mock
    private PromptFactory promptFactory;
    @Mock
    private DecisionMessageBuilder decisionMessageBuilder;
    @Mock
    private SessionStore sessionStore;

    private CaseService caseService;

    private static final byte[] FAKE_IMAGE = new byte[]{1, 2, 3};
    private static final String MIME_JPEG = "image/jpeg";

    private CaseRequest complaintRequest() {
        return new CaseRequest(
                RequestType.COMPLAINT,
                EquipmentCategory.SMARTFONY,
                "Samsung Galaxy S24",
                LocalDate.of(2024, 1, 1),
                "Pęknięty ekran"
        );
    }

    private CaseRequest returnRequest() {
        return new CaseRequest(
                RequestType.RETURN,
                EquipmentCategory.LAPTOPY_KOMPUTERY,
                "Dell XPS 15",
                LocalDate.of(2024, 6, 1),
                null
        );
    }

    private ImageAssessment okAssessment(RequestType requestType) {
        return new ImageAssessment(
                requestType, "Opis", true, "Pęknięty ekran", "Uderzenie", null, null,
                ImageQuality.OK, "raw"
        );
    }

    private Decision acceptedDecision() {
        return new Decision(Outcome.UZNANA, "Uzasadnienie", List.of("Krok 1"), List.of(), true);
    }

    private Decision returnAcceptedDecision() {
        return new Decision(Outcome.PRZYJETY_DO_ODSPRZEDAZY, "Uzasadnienie zwrotu",
                List.of("Krok 1"), List.of(), true);
    }

    @BeforeEach
    void setUp() {
        caseService = new CaseService(
                imageValidator, imageCompressor, llmClient, policyProvider,
                promptFactory, decisionMessageBuilder, sessionStore
        );
    }

    @Test
    @DisplayName("valid COMPLAINT request returns CaseResult with correct outcome and sessionId")
    void createCase_validComplaint_returnsCorrectResult() {
        var request = complaintRequest();
        var compressed = new CompressedImage(FAKE_IMAGE, MIME_JPEG, "data:image/jpeg;base64,AAEC");
        var assessment = okAssessment(RequestType.COMPLAINT);
        var decision = acceptedDecision();

        when(imageCompressor.compress(FAKE_IMAGE, MIME_JPEG)).thenReturn(compressed);
        when(policyProvider.load(RequestType.COMPLAINT)).thenReturn("Policy text");
        when(promptFactory.imagePrompt(RequestType.COMPLAINT))
                .thenReturn(new PromptFactory.Prompt("system", "user"));
        when(llmClient.analyzeImage(compressed.dataUrl(), MIME_JPEG, "user")).thenReturn(assessment);
        when(promptFactory.reasoningPrompt(eq(RequestType.COMPLAINT), eq(assessment),
                any(CaseContext.class), eq("Policy text"))).thenReturn("Reasoning prompt");
        when(llmClient.decide("Reasoning prompt", RequestType.COMPLAINT)).thenReturn(decision);
        when(decisionMessageBuilder.build(decision)).thenReturn("## Decyzja: Uznana");

        CaseResult result = caseService.createCase(request, FAKE_IMAGE, MIME_JPEG);

        assertThat(result).isNotNull();
        assertThat(result.sessionId()).isNotNull();
        assertThat(result.outcome()).isEqualTo(Outcome.UZNANA);
        assertThat(result.decisionMessageMarkdown()).isEqualTo("## Decyzja: Uznana");
        assertThat(result.decision()).isEqualTo(decision);
    }

    @Test
    @DisplayName("valid RETURN request returns CaseResult with correct outcome")
    void createCase_validReturn_returnsCorrectResult() {
        var request = returnRequest();
        var compressed = new CompressedImage(FAKE_IMAGE, MIME_JPEG, "data:image/jpeg;base64,AAEC");
        var assessment = okAssessment(RequestType.RETURN);
        var decision = returnAcceptedDecision();

        when(imageCompressor.compress(FAKE_IMAGE, MIME_JPEG)).thenReturn(compressed);
        when(policyProvider.load(RequestType.RETURN)).thenReturn("Return policy");
        when(promptFactory.imagePrompt(RequestType.RETURN))
                .thenReturn(new PromptFactory.Prompt("sys", "usr"));
        when(llmClient.analyzeImage(compressed.dataUrl(), MIME_JPEG, "usr")).thenReturn(assessment);
        when(promptFactory.reasoningPrompt(eq(RequestType.RETURN), eq(assessment),
                any(CaseContext.class), eq("Return policy"))).thenReturn("Return reasoning");
        when(llmClient.decide("Return reasoning", RequestType.RETURN)).thenReturn(decision);
        when(decisionMessageBuilder.build(decision)).thenReturn("## Decyzja: Przyjęty do odsprzedaży");

        CaseResult result = caseService.createCase(request, FAKE_IMAGE, MIME_JPEG);

        assertThat(result.outcome()).isEqualTo(Outcome.PRZYJETY_DO_ODSPRZEDAZY);
    }

    @Test
    @DisplayName("validation failure throws exception and makes zero LLM calls")
    void createCase_validationFailure_throwsAndNoLlmCalls() {
        var request = complaintRequest();
        doThrow(new UnsupportedImageTypeException("image/gif", "Nieobsługiwany typ"))
                .when(imageValidator).validate(FAKE_IMAGE, "image/gif", "upload");

        assertThatThrownBy(() -> caseService.createCase(request, FAKE_IMAGE, "image/gif"))
                .isInstanceOf(UnsupportedImageTypeException.class);

        verifyNoInteractions(llmClient);
        verifyNoInteractions(imageCompressor);
    }

    @Test
    @DisplayName("vision call happens before reasoning call — strict ordering")
    void createCase_visionCalledBeforeReasoning() {
        var request = complaintRequest();
        var compressed = new CompressedImage(FAKE_IMAGE, MIME_JPEG, "data:image/jpeg;base64,AAEC");
        var assessment = okAssessment(RequestType.COMPLAINT);
        var decision = acceptedDecision();

        when(imageCompressor.compress(FAKE_IMAGE, MIME_JPEG)).thenReturn(compressed);
        when(policyProvider.load(RequestType.COMPLAINT)).thenReturn("Policy");
        when(promptFactory.imagePrompt(RequestType.COMPLAINT))
                .thenReturn(new PromptFactory.Prompt("s", "u"));
        when(llmClient.analyzeImage(compressed.dataUrl(), MIME_JPEG, "u")).thenReturn(assessment);
        when(promptFactory.reasoningPrompt(any(), any(), any(), any())).thenReturn("Reasoning");
        when(llmClient.decide("Reasoning", RequestType.COMPLAINT)).thenReturn(decision);
        when(decisionMessageBuilder.build(decision)).thenReturn("msg");

        caseService.createCase(request, FAKE_IMAGE, MIME_JPEG);

        InOrder inOrder = inOrder(llmClient);
        inOrder.verify(llmClient).analyzeImage(anyString(), anyString(), anyString());
        inOrder.verify(llmClient).decide(anyString(), any(RequestType.class));
    }

    @Test
    @DisplayName("session is saved with assistant message after pipeline completes")
    void createCase_sessionSavedWithAssistantMessage() {
        var request = complaintRequest();
        var compressed = new CompressedImage(FAKE_IMAGE, MIME_JPEG, "data:image/jpeg;base64,AAEC");
        var assessment = okAssessment(RequestType.COMPLAINT);
        var decision = acceptedDecision();

        when(imageCompressor.compress(FAKE_IMAGE, MIME_JPEG)).thenReturn(compressed);
        when(policyProvider.load(RequestType.COMPLAINT)).thenReturn("Policy");
        when(promptFactory.imagePrompt(RequestType.COMPLAINT))
                .thenReturn(new PromptFactory.Prompt("s", "u"));
        when(llmClient.analyzeImage(compressed.dataUrl(), MIME_JPEG, "u")).thenReturn(assessment);
        when(promptFactory.reasoningPrompt(any(), any(), any(), any())).thenReturn("Reasoning");
        when(llmClient.decide("Reasoning", RequestType.COMPLAINT)).thenReturn(decision);
        when(decisionMessageBuilder.build(decision)).thenReturn("## Decyzja");

        caseService.createCase(request, FAKE_IMAGE, MIME_JPEG);

        verify(sessionStore).save(argThat(session ->
                session.messages() != null
                && !session.messages().isEmpty()
                && session.messages().get(0).role() == MessageRole.ASSISTANT
                && session.messages().get(0).content().equals("## Decyzja")
        ));
    }

    @Test
    @DisplayName("POOR_UNREADABLE image assessment is passed to reasoning prompt")
    void createCase_poorImageAssessment_passedToReasoningPrompt() {
        var request = complaintRequest();
        var compressed = new CompressedImage(FAKE_IMAGE, MIME_JPEG, "data:image/jpeg;base64,AAEC");
        var poorAssessment = new ImageAssessment(
                RequestType.COMPLAINT, "Nieczytelne", null, null, null, null, null,
                ImageQuality.POOR_UNREADABLE, "raw"
        );
        var decision = new Decision(Outcome.WYMAGA_WERYFIKACJI, "Nieczytelne zdjęcie",
                List.of("Prześlij zdjęcie"), List.of("Wyraźne zdjęcie"), true);

        when(imageCompressor.compress(FAKE_IMAGE, MIME_JPEG)).thenReturn(compressed);
        when(policyProvider.load(RequestType.COMPLAINT)).thenReturn("Policy");
        when(promptFactory.imagePrompt(RequestType.COMPLAINT))
                .thenReturn(new PromptFactory.Prompt("s", "u"));
        when(llmClient.analyzeImage(any(), any(), any())).thenReturn(poorAssessment);
        when(promptFactory.reasoningPrompt(eq(RequestType.COMPLAINT), eq(poorAssessment),
                any(), any())).thenReturn("Reasoning with poor image");
        when(llmClient.decide("Reasoning with poor image", RequestType.COMPLAINT)).thenReturn(decision);
        when(decisionMessageBuilder.build(decision)).thenReturn("## Wymaga weryfikacji");

        CaseResult result = caseService.createCase(request, FAKE_IMAGE, MIME_JPEG);

        assertThat(result.outcome()).isEqualTo(Outcome.WYMAGA_WERYFIKACJI);
        verify(promptFactory).reasoningPrompt(eq(RequestType.COMPLAINT), eq(poorAssessment), any(), any());
    }
}

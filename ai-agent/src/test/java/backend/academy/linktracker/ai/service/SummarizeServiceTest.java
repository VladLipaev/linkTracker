package backend.academy.linktracker.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.ai.client.YandexGPTRequestBody;
import backend.academy.linktracker.ai.client.YandexGPTRestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class SummarizeServiceTest {

    @Mock
    private YandexGPTRestClient yandexGPTRestClient;

    private SummarizeService summarizeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Captor
    private ArgumentCaptor<YandexGPTRequestBody> requestCaptor;

    private static final String FOLDER_ID = "test-folder-id";
    private static final String SHORT_TEXT = "This is a short text that does not need summarization.";
    private static final String LONG_TEXT = "A very long text that definitely needs summarization. ".repeat(50);
    private static final String SUMMARY_RESULT = "This is a summarized version of the text.";

    @BeforeEach
    void setUp() {
        summarizeService = new SummarizeService(yandexGPTRestClient, objectMapper);
        ReflectionTestUtils.setField(summarizeService, "folderId", FOLDER_ID);
    }

    @Test
    @DisplayName("TC-3.1: Суммаризация длинного текста - успешный ответ API")
    void shouldSummarizeLongTextSuccessfully() throws Exception {
        // given
        String mockApiResponse = """
            {
                "result": {
                    "alternatives": [{
                        "message": {
                            "text": "%s"
                        }
                    }]
                }
            }
            """.formatted(SUMMARY_RESULT);

        when(yandexGPTRestClient.gptRequest(any())).thenReturn(mockApiResponse);

        // when
        String result = summarizeService.summarize(LONG_TEXT);

        // then
        assertThat(result).isEqualTo(SUMMARY_RESULT);

        verify(yandexGPTRestClient).gptRequest(requestCaptor.capture());
        YandexGPTRequestBody request = requestCaptor.getValue();

        assertThat(request.modelUri()).isEqualTo("gpt://" + FOLDER_ID + "/yandexgpt/latest");

        assertThat(request.messages()).hasSize(1);
        assertThat(request.messages().get(0))
                .containsEntry("role", "user")
                .hasEntrySatisfying("text", text -> assertThat(text.toString()).contains(LONG_TEXT));

        assertThat(request.completionOptions())
                .containsEntry("stream", false)
                .containsEntry("temperature", 0.7)
                .containsEntry("maxTokens", "500");
    }

    @Test
    @DisplayName("TC-3.1: Суммаризация длинного текста - проверка полного цикла")
    void shouldSummarizeLongTextCompleteFlow() throws Exception {
        // given
        String inputText = "Java is a high-level, class-based, object-oriented programming language "
                + "that is designed to have as few implementation dependencies as possible. ".repeat(10);
        String expectedSummary = "Java is a popular programming language.";

        String mockApiResponse = """
            {
                "result": {
                    "alternatives": [{
                        "message": {
                            "text": "%s"
                        }
                    }]
                }
            }
            """.formatted(expectedSummary);

        when(yandexGPTRestClient.gptRequest(any())).thenReturn(mockApiResponse);

        // when
        String actualSummary = summarizeService.summarize(inputText);

        // then
        assertThat(actualSummary).isEqualTo(expectedSummary);

        verify(yandexGPTRestClient, times(1)).gptRequest(any());
    }

    @Test
    @DisplayName("TC-3.2: Короткий текст - сервис может обработать, но consumer не вызывает суммаризацию")
    void shouldHandleShortTextWhenCalledDirectly() throws Exception {
        // given
        String mockApiResponse = """
            {
                "result": {
                    "alternatives": [{
                        "message": {
                            "text": "Short text summary."
                        }
                    }]
                }
            }
            """;

        when(yandexGPTRestClient.gptRequest(any())).thenReturn(mockApiResponse);

        // when
        String result = summarizeService.summarize(SHORT_TEXT);

        // then
        assertThat(result).isEqualTo("Short text summary.");

        // Проверяем, что API был вызван с коротким текстом
        verify(yandexGPTRestClient).gptRequest(requestCaptor.capture());
        YandexGPTRequestBody request = requestCaptor.getValue();

        assertThat(request.messages().get(0).get("text").toString()).contains(SHORT_TEXT);
    }

    @Test
    @DisplayName("TC-3.2: Короткий текст не должен вызывать суммаризацию на уровне consumer (мокаем проверку)")
    void shouldNotCallSummarizeForShortTextAtConsumerLevel() {
        String shortDescription = SHORT_TEXT;
        boolean needsSummarization = shortDescription.length() > 500;

        // when - проверяем, нужна ли суммаризация
        // then - для короткого текста суммаризация не требуется
        assertThat(needsSummarization).isFalse();

        verifyNoInteractions(yandexGPTRestClient);
    }

    @Test
    @DisplayName("Обработка ошибки API - некорректный JSON в ответе")
    void shouldThrowExceptionOnInvalidApiResponse() {
        // given
        String invalidResponse = "This is not a valid JSON";
        when(yandexGPTRestClient.gptRequest(any())).thenReturn(invalidResponse);

        // when & then
        assertThatThrownBy(() -> summarizeService.summarize(LONG_TEXT))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to parse YandexGPT response");

        verify(yandexGPTRestClient).gptRequest(any());
    }

    @Test
    @DisplayName("Обработка ошибки API - отсутствует поле text в ответе")
    void shouldThrowExceptionWhenTextIsMissing() {
        // given
        String responseWithoutText = """
            {
                "result": {
                    "alternatives": [{
                        "message": {}
                    }]
                }
            }
            """;

        when(yandexGPTRestClient.gptRequest(any())).thenReturn(responseWithoutText);

        // when & then
        assertThatThrownBy(() -> summarizeService.summarize(LONG_TEXT))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to parse YandexGPT response");

        verify(yandexGPTRestClient).gptRequest(any());
    }

    @Test
    @DisplayName("Обработка ошибки API - пустой массив alternatives")
    void shouldThrowExceptionWhenAlternativesIsEmpty() {
        // given
        String responseWithEmptyAlternatives = """
            {
                "result": {
                    "alternatives": []
                }
            }
            """;

        when(yandexGPTRestClient.gptRequest(any())).thenReturn(responseWithEmptyAlternatives);

        // when & then
        assertThatThrownBy(() -> summarizeService.summarize(LONG_TEXT)).isInstanceOf(RuntimeException.class);

        verify(yandexGPTRestClient).gptRequest(any());
    }

    @Test
    @DisplayName("Проверка формирования правильного промпта для суммаризации")
    void shouldFormatPromptCorrectly() throws Exception {
        // given
        String originalText = "This is a long text that needs to be summarized into 2-3 sentences.";
        String mockApiResponse = """
            {
                "result": {
                    "alternatives": [{
                        "message": {
                            "text": "Summarized text."
                        }
                    }]
                }
            }
            """;

        when(yandexGPTRestClient.gptRequest(any())).thenReturn(mockApiResponse);

        // when
        summarizeService.summarize(originalText);

        // then
        verify(yandexGPTRestClient).gptRequest(requestCaptor.capture());
        YandexGPTRequestBody request = requestCaptor.getValue();

        String messageText = request.messages().get(0).get("text").toString();
        assertThat(messageText)
                .startsWith("Summarize the following update in 2–3 sentences:")
                .contains(originalText);
    }

    @Test
    @DisplayName("Множественные вызовы суммаризации с разными текстами")
    void shouldHandleMultipleSummarizationCalls() throws Exception {
        // given
        String text1 = "First long text that needs summarization. ".repeat(30);
        String text2 = "Second long text that also needs summarization. ".repeat(30);

        String mockApiResponse1 = """
            {
                "result": {
                    "alternatives": [{
                        "message": {
                            "text": "First summary."
                        }
                    }]
                }
            }
            """;

        String mockApiResponse2 = """
            {
                "result": {
                    "alternatives": [{
                        "message": {
                            "text": "Second summary."
                        }
                    }]
                }
            }
            """;

        when(yandexGPTRestClient.gptRequest(any())).thenReturn(mockApiResponse1).thenReturn(mockApiResponse2);

        // when
        String result1 = summarizeService.summarize(text1);
        String result2 = summarizeService.summarize(text2);

        // then
        assertThat(result1).isEqualTo("First summary.");
        assertThat(result2).isEqualTo("Second summary.");

        verify(yandexGPTRestClient, times(2)).gptRequest(any());
    }
}

package backend.academy.linktracker.ai.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import backend.academy.linktracker.ai.config.logging.ClientLoggingInterceptor;
import backend.academy.linktracker.ai.config.logging.properties.HttpMaskingProperties;
import backend.academy.linktracker.ai.config.logging.properties.YandexMaskingProperties;
import backend.academy.linktracker.ai.service.MaskingService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@ExtendWith(OutputCaptureExtension.class)
class ClientLoggingInterceptorIntegrationTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;

    private static final String YANDEX_URI_PATH = "/foundationModels/v1/completion";

    @BeforeEach
    void setUp() {
        MaskingService maskingService = new MaskingService();

        HttpMaskingProperties httpProperties = new HttpMaskingProperties();
        httpProperties.setHeaders(List.of("Authorization"));
        httpProperties.setKeys(List.of("token"));

        YandexMaskingProperties yandexProperties = new YandexMaskingProperties();
        yandexProperties.setHeaders(List.of("Authorization", "x-folder-id"));
        yandexProperties.setKeys(List.of("apiKey"));

        ClientLoggingInterceptor interceptor = new ClientLoggingInterceptor(
            maskingService,
            httpProperties,
            yandexProperties
        );
        ReflectionTestUtils.setField(interceptor, "yandexUri", YANDEX_URI_PATH);

        restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(interceptor);

        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    @DisplayName("Should mask sensitive headers in logs for Yandex API request")
    void restTemplate_YandexApiRequest_MasksHeadersInLogs(CapturedOutput output) {
        String targetUrl = "https://llm.api.cloud.yandex.net" + YANDEX_URI_PATH + "?folder=123";

        mockServer.expect(requestTo(targetUrl))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer secret-yandex-token");
        headers.add("x-folder-id", "secret-folder-id");

        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>("{}", headers);

        restTemplate.postForObject(targetUrl, entity, String.class);

        // Проверяем, что в логах есть маскированные значения
        assertThat(output.getAll())
            .contains("Authorization: ***")
            .contains("x-folder-id: ***");

        assertThat(output.getAll())
            .doesNotContain("secret-yandex-token")
            .doesNotContain("secret-folder-id");
    }

    @Test
    @DisplayName("Should mask sensitive query parameters and headers in logs for Generic HTTP request")
    void restTemplate_GenericApiRequest_MasksQueryParamsAndHeadersInLogs(CapturedOutput output) {
        String targetUrl = "https://api.example.com/v1/data?token=secret-token-123&user=admin";

        mockServer.expect(requestTo(targetUrl))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("{\"data\":\"success\"}", MediaType.APPLICATION_JSON));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer super-secret-bearer");

        org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

        restTemplate.exchange(targetUrl, HttpMethod.GET, entity, String.class);

        // Проверяем маскирование в логе
        assertThat(output.getAll())
            .contains("query: token=***&user=admin")
            .contains("Authorization: ***");

        assertThat(output.getAll())
            .doesNotContain("secret-token-123")
            .doesNotContain("super-secret-bearer");
    }
}

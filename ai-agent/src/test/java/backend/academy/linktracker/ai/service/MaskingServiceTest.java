package backend.academy.linktracker.ai.service;


import backend.academy.linktracker.ai.config.logging.properties.HttpMaskingProperties;
import backend.academy.linktracker.ai.config.logging.properties.YandexMaskingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MaskingServiceTest {

    private MaskingService maskingService;
    private HttpMaskingProperties httpProperties;
    private YandexMaskingProperties yandexProperties;

    @BeforeEach
    void setUp() {
        maskingService = new MaskingService();

        httpProperties = new HttpMaskingProperties();
        httpProperties.setHeaders(List.of("Authorization", "X-Api-Key"));
        httpProperties.setKeys(List.of("token", "secret"));

        yandexProperties = new YandexMaskingProperties();
        yandexProperties.setHeaders(List.of("Authorization", "x-folder-id"));
        yandexProperties.setKeys(List.of("apiKey"));
    }

    @Test
    @DisplayName("Should return empty string when query is null")
    void maskQuery_NullQuery_ReturnsEmptyString() {
        String result = maskingService.maskQuery(null, httpProperties);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should mask query parameters that match sensitive keys (case insensitive)")
    void maskQuery_SensitiveKeys_MasksValues() {
        String query = "user=john&TOKEN=12345&secret_code=abc&page=1";

        String result = maskingService.maskQuery(query, httpProperties);

        assertThat(result).isEqualTo("user=john&TOKEN=***&secret_code=***&page=1");
    }

    @Test
    @DisplayName("Should return {} when headers are null or empty")
    void maskHeaders_NullOrEmptyHeaders_ReturnsEmptyBraces() {
        assertThat(maskingService.maskHeaders(null, httpProperties)).isEqualTo("{}");
        assertThat(maskingService.maskHeaders(new HttpHeaders(), httpProperties)).isEqualTo("{}");
    }

    @Test
    @DisplayName("Should mask sensitive headers according to properties")
    void maskHeaders_SensitiveHeaders_MasksValues() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer my-secret-token");
        headers.add("X-Api-Key", "12345");
        headers.add("Content-Type", "application/json");

        String result = maskingService.maskHeaders(headers, httpProperties);

        assertThat(result)
            .contains("Authorization: ***")
            .contains("X-Api-Key: ***")
            .contains("Content-Type: application/json");
    }

    @Test
    @DisplayName("Should mask headers using Yandex masking properties correctly")
    void maskHeaders_YandexProperties_MasksFolderIdAndAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer yandex-token");
        headers.add("x-folder-id", "folder-12345");
        headers.add("Accept", "application/json");

        String result = maskingService.maskHeaders(headers, yandexProperties);

        assertThat(result)
            .contains("Authorization: ***")
            .contains("x-folder-id: ***")
            .contains("Accept: application/json");
    }

    @Test
    @DisplayName("Should return empty string when URI is null")
    void maskURI_NullUri_ReturnsEmptyString() {
        String result = maskingService.maskURI(null, httpProperties);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should mask URI path segments starting with configured sensitive keys")
    void maskURI_SensitivePathSegments_MasksSegments() {
        String uri = "/api/v1/token_123/details/secret_999";

        String result = maskingService.maskURI(uri, httpProperties);

        assertThat(result).isEqualTo("/api/v1/token_123***/details/secret_999***");
    }
}

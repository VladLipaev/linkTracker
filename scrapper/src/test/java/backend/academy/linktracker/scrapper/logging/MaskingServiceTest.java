package backend.academy.linktracker.scrapper.logging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import backend.academy.linktracker.scrapper.properties.MaskingProperties;
import backend.academy.linktracker.scrapper.service.MaskingService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class MaskingServiceTest {

    private MaskingService maskingService;
    private MaskingProperties maskingProperties;

    @BeforeEach
    void setUp() {
        maskingService = new MaskingService();
        maskingProperties = new MaskingProperties() {
            @Override
            public List<String> getHeaders() {
                return List.of("Authorization", "X-Api-Key");
            }

            @Override
            public List<String> getKeys() {
                return List.of("key", "token", "secret");
            }
        };
    }

    @Test
    @DisplayName("Маскирование параметров в query-строке")
    void shouldMaskQueryParams() {
        String query = "user=john&token=12345&key=secret_value&page=1";

        String result = maskingService.maskQuery(query, maskingProperties);

        assertEquals("user=john&token=***&key=***&page=1", result);
    }

    @Test
    @DisplayName("Возврат пустой строки при передаче null в query")
    void shouldReturnEmptyStringForNullQuery() {
        String result = maskingService.maskQuery(null, maskingProperties);

        assertEquals("", result);
    }

    @Test
    @DisplayName("Маскирование чувствительных HTTP-заголовков")
    void shouldMaskSensitiveHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer my-secret-token");
        headers.add("X-Api-Key", "12345");
        headers.add("Content-Type", "application/json");

        String result = maskingService.maskHeaders(headers, maskingProperties);

        assertAll(
            () -> assertEquals(true, result.contains("Authorization: ***")),
            () -> assertEquals(true, result.contains("X-Api-Key: ***")),
            () -> assertEquals(true, result.contains("Content-Type: application/json"))
        );
    }
}

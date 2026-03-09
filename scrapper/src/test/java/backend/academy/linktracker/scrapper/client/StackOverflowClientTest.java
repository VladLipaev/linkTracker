package backend.academy.linktracker.scrapper.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

@WireMockTest(httpPort = 54321)
class StackOverflowClientTest {

    private StackOverflowClient stackOverflowClient;

    @BeforeEach
    public void setUp() {
        RestClient restClient =
                RestClient.builder().baseUrl("http://localhost:54321").build();
        stackOverflowClient = new StackOverflowClient(restClient, null);
        StackoverflowProperties properties = new StackoverflowProperties();
        properties.setKey("test-key");

        stackOverflowClient = new StackOverflowClient(restClient, properties);
    }

    @Test
    void fetchQuestion_serverReturns404_shouldThrowRuntimeException() {
        String questionId = "99999999";

        stubFor(get(urlPathEqualTo("/questions/" + questionId))
                .withQueryParam("site", equalTo("stackoverflow"))
                .withQueryParam("key", equalTo("test-key"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error_id\": 404, \"error_message\": \"no item found\"}")));

        // Act & Assert
        StackOverflowException exception = assertThrows(StackOverflowException.class, () -> {
            stackOverflowClient.fetchQuestion(questionId);
        });

        assertTrue(exception.getMessage().contains("404 NOT_FOUND"));
    }

    @Test
    void fetchQuestion_invalidJsonSchema_shouldThrowException() {
        String questionId = "123456";
        String invalidJson = "<xml>Я не JSON, я ломаю парсер!</xml>";

        stubFor(get(urlPathEqualTo("/questions/" + questionId))
                .withQueryParam("site", equalTo("stackoverflow"))
                .withQueryParam("key", equalTo("test-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(invalidJson)));

        assertThrows(StackOverflowException.class, () -> {
            stackOverflowClient.fetchQuestion(questionId);
        });
    }
}

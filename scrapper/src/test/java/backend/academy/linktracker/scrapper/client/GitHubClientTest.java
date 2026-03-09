package backend.academy.linktracker.scrapper.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

@WireMockTest(httpPort = 54321)
class GitHubClientTest {

    private GitHubClient gitHubClient;

    @BeforeEach
    void setUp() {
        // Создаем RestClient, который смотрит на локальный WireMock вместо реального GitHub
        RestClient restClient =
                RestClient.builder().baseUrl("http://localhost:54321").build();

        gitHubClient = new GitHubClient(restClient);
    }

    @Test
    void fetchRepo_serverReturns500_shouldThrowRuntimeException() {
        // Arrange: Настраиваем WireMock на возврат ошибки 500
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Internal Server Error\"}")));

        // Act & Assert: Проверяем, что клиент выбросил наше кастомное исключение из .onStatus
        GitHubClientException exception = assertThrows(GitHubClientException.class, () -> {
            gitHubClient.fetchRepo("test-owner", "test-repo");
        });
        WireMock.verify(WireMock.getRequestedFor(WireMock.urlPathMatching("/repos/test-owner/test-repo")));
        assertTrue(exception.getMessage().contains("500 INTERNAL_SERVER_ERROR"));
    }

    @Test
    void fetchRepo_missingRequiredFields_shouldThrowException() {
        // нет поля updatedAt
        String invalidSchemaJson = "{ \"id\": 12345, \"name\": \"test-repo\" }";

        stubFor(get(urlEqualTo("/repos/test-owner/test-repo"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(invalidSchemaJson)));

        GitHubClientException exception = assertThrows(GitHubClientException.class, () -> {
            gitHubClient.fetchRepo("test-owner", "test-repo");
        });
        WireMock.verify(WireMock.getRequestedFor(WireMock.urlPathMatching("/repos/test-owner/test-repo")));
        assertTrue(exception.getMessage().contains("не соответствует заявленной схеме"));
    }
}

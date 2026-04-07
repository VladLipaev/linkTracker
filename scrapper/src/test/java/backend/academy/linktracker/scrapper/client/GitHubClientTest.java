package backend.academy.linktracker.scrapper.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.time.OffsetDateTime;
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
            gitHubClient.fetchRepo("test-owner", "test-repo", OffsetDateTime.MIN);
        });
        WireMock.verify(WireMock.getRequestedFor(WireMock.urlPathEqualTo("/repos/test-owner/test-repo/issues"))
                .withQueryParam("state", equalTo("all"))
                .withQueryParam("since", equalTo("-999999999-01-01T00:00+18:00")));
        assertTrue(exception.getMessage().contains("GitHub API error: "));
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
            gitHubClient.fetchRepo("test-owner", "test-repo", OffsetDateTime.MIN);
        });
        WireMock.verify(WireMock.getRequestedFor(WireMock.urlPathEqualTo("/repos/test-owner/test-repo/issues"))
                .withQueryParam("state", equalTo("all"))
                .withQueryParam("since", equalTo("-999999999-01-01T00:00+18:00")));
        assertTrue(exception.getMessage().contains("GitHub API error: "));
    }
}

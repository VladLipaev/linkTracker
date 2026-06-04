package backend.academy.linktracker.ai.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
public class YandexGPTRestClient {

    private final RestClient restClient;

    @Value("${app.yandexgpt.api-key}")
    private String apiKey;

    @Value("${app.yandexgpt.folder-id}")
    private String folderId;

    @Retry(name = "external-exponent")
    @CircuitBreaker(name = "external")
    public String gptRequest(YandexGPTRequestBody yandexGPTRequestBody) {
        return restClient
                .post()
                .uri("/foundationModels/v1/completion")
                .headers(httpHeaders -> {
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    httpHeaders.set("Authorization", "Api-Key " + apiKey);
                    httpHeaders.set("x-folder-id", folderId);
                })
                .body(yandexGPTRequestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    String errorBody = new String(response.getBody().readAllBytes());
                    throw new YandexGPTApiException(
                            "YandexGPT API error: " + response.getStatusCode() + " - " + errorBody);
                })
                .body(String.class);
    }
}

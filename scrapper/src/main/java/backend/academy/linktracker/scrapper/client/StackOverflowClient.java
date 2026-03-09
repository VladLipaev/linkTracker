package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.StackOverflowResponse;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@RequiredArgsConstructor
public class StackOverflowClient {

    private final RestClient restClient;
    private final StackoverflowProperties properties;

    public StackOverflowResponse fetchQuestion(String id) {
        try {
            StackOverflowResponse response = this.restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/questions/{id}")
                            .queryParam("site", "stackoverflow")
                            .queryParam("key", properties.getKey())
                            .build(id))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, res) -> {
                        throw new StackOverflowException("StackOverflow API error: " + res.getStatusCode());
                    })
                    .body(StackOverflowResponse.class);
            if (response == null || response.items() == null) {
                throw new StackOverflowException(
                        "StackOverflow API error: Тело ответа не соответствует заявленной схеме");
            }
            return response;
        } catch (RestClientException e) {
            throw new StackOverflowException("StackOverflow API error: " + e.getMessage());
        }
    }
}

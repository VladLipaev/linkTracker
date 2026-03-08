package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.StackOverflowResponse;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
public class StackOverflowClient {

    private final RestClient restClient;
    private final StackoverflowProperties properties;

    public StackOverflowResponse fetchQuestion(String id) {
        return this.restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/questions/{id}")
                        .queryParam("site", "stackoverflow")
                        .queryParam("key", properties.getKey())
                        .build(id))
                .retrieve()
                .body(StackOverflowResponse.class);
    }
}

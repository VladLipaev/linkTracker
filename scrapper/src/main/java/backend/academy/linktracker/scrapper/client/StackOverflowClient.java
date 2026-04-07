package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.handler.dto.StackOverflowResponse;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@RequiredArgsConstructor
public class StackOverflowClient {

    private final RestClient restClient;
    private final StackoverflowProperties properties;

    public StackOverflowResponse<StackOverflowResponse.QuestionItem> fetchQuestion(String id) {
        try {
            StackOverflowResponse<StackOverflowResponse.QuestionItem> response = this.restClient
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
                    .body(new ParameterizedTypeReference<>() {});
            ClientRequestLogging.handleRequestSuccess("Успешный запрос в SO", "stack_overflow_request", "success");
            return response;
        } catch (RestClientException e) {
            ClientRequestLogging.handleRequestFailure("Неудачный запрос в SO", "stack_overflow_request", "failure", e);
            throw new StackOverflowException("StackOverflow API error: " + e.getMessage());
        }
    }

    public StackOverflowResponse<StackOverflowResponse.ActivityItem> fetchAnswers(String questionId, long fromDate) {
        try {
            StackOverflowResponse<StackOverflowResponse.ActivityItem> response = this.restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/questions/{id}/answers")
                            .queryParam("site", "stackoverflow")
                            .queryParam("key", properties.getKey())
                            .queryParam("fromdate", fromDate)
                            .queryParam("filter", "!nNPvSNe7Gv")
                            .build(questionId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, res) -> {
                        throw new StackOverflowException("StackOverflow API error: " + res.getStatusCode());
                    })
                    .body(new ParameterizedTypeReference<>() {});
            ClientRequestLogging.handleRequestSuccess("Успешный запрос в SO", "stack_overflow_request", "success");
            return response;
        } catch (RestClientException e) {
            ClientRequestLogging.handleRequestFailure("Неудачный запрос в SO", "stack_overflow_request", "failure", e);
            throw new StackOverflowException("StackOverflow API error: " + e.getMessage());
        }
    }

    public StackOverflowResponse<StackOverflowResponse.ActivityItem> fetchComments(
            String questionId, long fromDateSec) {
        try {
            StackOverflowResponse<StackOverflowResponse.ActivityItem> response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/questions/{id}/comments")
                            .queryParam("site", "stackoverflow")
                            .queryParam("fromdate", fromDateSec)
                            .queryParam("filter", "!6WPIomp-eb(U5")
                            .build(questionId))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            ClientRequestLogging.handleRequestSuccess("Успешный запрос в SO", "stack_overflow_request", "success");
            return response;
        } catch (RestClientException e) {
            ClientRequestLogging.handleRequestFailure("Неудачный запрос в SO", "stack_overflow_request", "failure", e);
            throw new StackOverflowException("StackOverflow API error: " + e.getMessage());
        }
    }
}

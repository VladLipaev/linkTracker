package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.handler.dto.StackOverflowResponse;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.net.URI;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

@RequiredArgsConstructor
public class StackOverflowClient {

    private final RestClient restClient;
    private final StackoverflowProperties properties;

    @Retry(name = "external-exponent")
    @CircuitBreaker(name = "external")
    public StackOverflowResponse<StackOverflowResponse.QuestionItem> fetchQuestion(String id) {
        return executeWithLogging(
                uriBuilder -> uriBuilder
                        .path("/questions/{id}")
                        .queryParam("site", "stackoverflow")
                        .queryParam("key", properties.getKey())
                        .build(id),
                new ParameterizedTypeReference<>() {});
    }

    @Retry(name = "external-exponent")
    @CircuitBreaker(name = "external")
    public StackOverflowResponse<StackOverflowResponse.ActivityItem> fetchAnswers(String questionId, long fromDate) {
        return executeWithLogging(
                uriBuilder -> uriBuilder
                        .path("/questions/{id}/answers")
                        .queryParam("site", "stackoverflow")
                        .queryParam("key", properties.getKey())
                        .queryParam("fromdate", fromDate)
                        .queryParam("filter", "!nNPvSNe7Gv")
                        .build(questionId),
                new ParameterizedTypeReference<>() {});
    }

    @Retry(name = "external-exponent")
    @CircuitBreaker(name = "external")
    public StackOverflowResponse<StackOverflowResponse.ActivityItem> fetchComments(
            String questionId, long fromDateSec) {
        return executeWithLogging(
                uriBuilder -> uriBuilder
                        .path("/questions/{id}/comments")
                        .queryParam("site", "stackoverflow")
                        .queryParam("fromdate", fromDateSec)
                        .queryParam("filter", "!6WPIomp-eb(U5")
                        .build(questionId),
                new ParameterizedTypeReference<>() {});
    }

    private <T> T executeWithLogging(
            Function<UriBuilder, URI> uriFunction, ParameterizedTypeReference<T> responseType) {
        try {
            T response = this.restClient
                    .get()
                    .uri(uriFunction)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, res) -> {
                        throw new StackOverflowException("StackOverflow API error: " + res.getStatusCode());
                    })
                    .body(responseType);

            ClientRequestLogging.handleRequestSuccess("Успешный запрос в SO", "stack_overflow_request", "success");
            return response;
        } catch (org.springframework.web.client.HttpServerErrorException
                | org.springframework.web.client.ResourceAccessException e) {
            ClientRequestLogging.handleRequestFailure("Неудачный запрос в SO", "stack_overflow_request", "failure", e);
            throw e;
        } catch (RestClientException e) {
            ClientRequestLogging.handleRequestFailure("Неудачный запрос в SO", "stack_overflow_request", "failure", e);
            throw new StackOverflowException("StackOverflow API error: " + e.getMessage());
        }
    }
}

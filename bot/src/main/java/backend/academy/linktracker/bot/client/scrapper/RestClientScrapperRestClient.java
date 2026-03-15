package backend.academy.linktracker.bot.client.scrapper;

import backend.academy.linktracker.bot.dto.AddLinkRequest;
import backend.academy.linktracker.bot.dto.ApiErrorResponse;
import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.dto.ListLinksResponse;
import backend.academy.linktracker.bot.dto.RemoveLinkRequest;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@RequiredArgsConstructor
@Slf4j
public class RestClientScrapperRestClient implements ScrapperClient {

    private final RestClient restClient;

    @Override
    public void registerChat(long chatId) {
        try {
            restClient.post().uri("/tg-chat/{id}", chatId).retrieve().toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw handleException(e);
        }
    }

    @Override
    public LinkResponse addLink(long chatId, String link, List<String> tags) {
        try {
            return restClient
                    .post()
                    .uri("/links")
                    .header("Tg-Chat-Id", String.valueOf(chatId))
                    .body(new AddLinkRequest(link, tags))
                    .retrieve()
                    .body(LinkResponse.class);
        } catch (RestClientResponseException e) {
            throw handleException(e);
        }
    }

    @Override
    public ListLinksResponse getLinks(long chatId, String tag) {
        try {
            return restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/links")
                            .queryParamIfPresent("tag", Optional.ofNullable(tag))
                            .build())
                    .header("Tg-Chat-Id", String.valueOf(chatId))
                    .retrieve()
                    .body(ListLinksResponse.class);
        } catch (RestClientResponseException e) {
            throw handleException(e);
        }
    }

    @Override
    public LinkResponse removeLink(long chatId, String url) {
        try {
            return restClient
                    .method(HttpMethod.DELETE)
                    .uri("/links")
                    .header("Tg-Chat-Id", String.valueOf(chatId))
                    .body(new RemoveLinkRequest(url))
                    .retrieve()
                    .body(LinkResponse.class);
        } catch (RestClientResponseException e) {
            throw handleException(e);
        }
    }

    private ScrapperClientException handleException(RestClientResponseException e) {
        ApiErrorResponse error = e.getResponseBodyAs(ApiErrorResponse.class);

        String description = (error != null) ? error.description() : "Неизвестная ошибка скраппера";

        log.atError()
                .setMessage("Запрос к scrapper api не получился")
                .addKeyValue("status_code", e.getStatusCode().value())
                .addKeyValue("error_description", description)
                .addKeyValue("exception_name", error != null ? error.exceptionName() : "N/A")
                .log();

        return new ScrapperClientException(description, e);
    }
}

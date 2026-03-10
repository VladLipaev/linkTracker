package backend.academy.linktracker.scrapper.client.bot;

import backend.academy.linktracker.scrapper.dto.ApiErrorResponse;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@RequiredArgsConstructor
@Slf4j
public class RestClientTelegramBotRestClient implements TelegramBotRestClient {

    private final RestClient restClient;

    @Override
    public void sendUpdate(LinkUpdate linkUpdate) {
        try {
            restClient.post().uri("/updates").body(linkUpdate).retrieve().toBodilessEntity();
        } catch (RestClientResponseException e) {
            ApiErrorResponse response = e.getResponseBodyAs(ApiErrorResponse.class);
            if (e.getStatusCode().is4xxClientError()) {
                log.atError()
                        .setMessage("сервис бота вернул ошибку клиента")
                        .addKeyValue("status", e.getStatusCode())
                        .addKeyValue("description", response != null ? response.description() : "no description")
                        .log();
                throw new BotClientException(response != null ? response.exceptionMessage() : "Bad Request");
            } else if (e.getStatusCode().is5xxServerError()) {
                log.atError()
                        .setMessage("сервис бота вернул ошибку сервера")
                        .addKeyValue("status", e.getStatusCode())
                        .addKeyValue("description", response != null ? response.description() : "no description")
                        .log();
                throw new BotServerException(response != null ? response.exceptionMessage() : "bot service error");
            }
        }
    }
}

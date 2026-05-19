package backend.academy.linktracker.scrapper.client.bot;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.service.kafka.KafkaNotificationUpdateSender;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
@Slf4j
public class RestClientTelegramBotClient implements TelegramBotClient {

    private final RestClient restClient;
    private final KafkaNotificationUpdateSender updateSender;

    @Override
    @RateLimiter(name = "bot")
    @CircuitBreaker(name = "bot", fallbackMethod = "fallbackSendUpdateKafka")
    public void sendUpdate(LinkUpdate linkUpdate) {
        restClient
                .post()
                .uri("/updates")
                .body(linkUpdate)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() && status.value() != 429,
                        (request, response) -> handleBotClientException(response))
                .toBodilessEntity();
        log.atInfo()
                .setMessage("Успешный запрос к сервису бота")
                .addKeyValue("event.type", "telegram_bot_request")
                .addKeyValue("event.status", "success")
                .log();
    }

    private void handleBotClientException(ClientHttpResponse response) throws IOException {
        String description = "Ошибка клиента: %s".formatted(response.getStatusText());

        log.atError()
                .setMessage("Бизнес-ошибка")
                .addKeyValue("message", description)
                .log();
        throw new BotClientException(description);
    }

    public void fallbackSendUpdateKafka(LinkUpdate linkUpdate, Throwable t) {

        if (t instanceof BotClientException botClientException) {
            throw botClientException;
        }

        log.atError()
                .setMessage("ошибка при отправке сообщения по rest, включается альтернативная кафка")
                .addKeyValue("error.message", t.getMessage())
                .log();
        updateSender.sendUpdate(linkUpdate);
    }
}

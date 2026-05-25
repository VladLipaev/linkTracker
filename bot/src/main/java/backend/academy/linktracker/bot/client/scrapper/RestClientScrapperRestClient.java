package backend.academy.linktracker.bot.client.scrapper;

import backend.academy.linktracker.bot.dto.AddLinkRequest;
import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.dto.ListLinksResponse;
import backend.academy.linktracker.bot.dto.RemoveLinkRequest;
import backend.academy.linktracker.bot.dto.avro.AddLinkMessageAvro;
import backend.academy.linktracker.bot.dto.avro.RegisterChatAvro;
import backend.academy.linktracker.bot.dto.avro.RemoveLinkMessageAvro;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
@Slf4j
public class RestClientScrapperRestClient implements ScrapperClient {

    private final RestClient restClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.communication.client.kafka-fallback.topic.chat-reg:chat-registration-topic}")
    private String chatRegTopic;

    @Value("${app.communication.client.kafka-fallback.topic.link-add:link-add-topic}")
    private String addLinkTopic;

    @Value("${app.communication.client.kafka-fallback.topic.link-remove:link-remove-topic}")
    private String removeLinkTopic;

    @Override
    @RateLimiter(name = "scrapper")
    @CircuitBreaker(name = "scrapper", fallbackMethod = "fallbackKafkaRegChat")
    public void registerChat(long chatId) {
        restClient
                .post()
                .uri("/tg-chat/{id}", chatId)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() && status.value() != 429, (request, response) -> {
                    handleScrapperClientException(response);
                })
                .toBodilessEntity();
    }

    @SneakyThrows
    public void fallbackKafkaRegChat(long chatId, Throwable t) {
        if (t instanceof ScrapperClientException scrapperClientException) {
            throw scrapperClientException;
        }
        log.atError()
                .setMessage("Ошибка при регистрации чата, Перенаправляем в Kafka")
                .addKeyValue("chatId", chatId)
                .addKeyValue("error.message", t.getMessage())
                .log();
        kafkaTemplate.send(chatRegTopic, new RegisterChatAvro(chatId));
    }

    @Override
    @RateLimiter(name = "scrapper")
    @CircuitBreaker(name = "scrapper", fallbackMethod = "fallbackKafkaAddLink")
    public LinkResponse addLink(long chatId, String link, List<String> tags) {
        return restClient
                .post()
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .body(new AddLinkRequest(link, tags))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() && status.value() != 429, (request, response) -> {
                    handleScrapperClientException(response);
                })
                .body(LinkResponse.class);
    }

    @SneakyThrows
    public LinkResponse fallbackKafkaAddLink(long chatId, String link, List<String> tags, Throwable t) {
        if (t instanceof ScrapperClientException scrapperClientException) {
            throw scrapperClientException;
        }
        log.atError()
                .setMessage("Ошибка при добавлении ссылки, Перенаправляем в Kafka")
                .addKeyValue("chatId", chatId)
                .addKeyValue("error.message", t.getMessage())
                .log();
        AddLinkMessageAvro message = AddLinkMessageAvro.newBuilder()
                .setChatId(chatId)
                .setUrl(link)
                .setTags(tags)
                .build();

        kafkaTemplate.send(addLinkTopic, message);
        throw new ScrapperClientException("На стороне сервера проблемы, но вашу ссылку мы поставили в очередь");
    }

    @Override
    @RateLimiter(name = "scrapper")
    @CircuitBreaker(name = "scrapper", fallbackMethod = "fallbackKafkaGetLinks")
    @Retry(name = "scrapper")
    public ListLinksResponse getLinks(long chatId, String tag) {
        return restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/links")
                        .queryParamIfPresent("tag", Optional.ofNullable(tag))
                        .build())
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() && status.value() != 429, (request, response) -> {
                    handleScrapperClientException(response);
                })
                .body(ListLinksResponse.class);
    }

    @SneakyThrows
    public ListLinksResponse fallbackKafkaGetLinks(long chatId, String tag, Throwable t) {
        if (t instanceof ScrapperClientException scrapperClientException) {
            throw scrapperClientException;
        }
        log.atError()
                .setMessage("Ошибка при попытке вернуть ссылки")
                .addKeyValue("chatId", chatId)
                .addKeyValue("tag", tag)
                .addKeyValue("error.message", t.getMessage())
                .log();
        throw new ScrapperClientException("На стороне сервера проблемы, подождите, мы работает над этим");
    }

    @Override
    @RateLimiter(name = "scrapper")
    @CircuitBreaker(name = "scrapper", fallbackMethod = "fallbackKafkaRemoveLink")
    @Retry(name = "scrapper")
    public LinkResponse removeLink(long chatId, String url) {
        return restClient
                .method(HttpMethod.DELETE)
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .body(new RemoveLinkRequest(url))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() && status.value() != 429, (request, response) -> {
                    handleScrapperClientException(response);
                })
                .body(LinkResponse.class);
    }

    @SneakyThrows
    public LinkResponse fallbackKafkaRemoveLink(long chatId, String url, Throwable t) {
        if (t instanceof ScrapperClientException scrapperClientException) {
            throw scrapperClientException;
        }
        log.atError()
                .setMessage("Ошибка при попытке удалить ссылку")
                .addKeyValue("chatId", chatId)
                .addKeyValue("url", url)
                .addKeyValue("error.message", t.getMessage())
                .log();

        kafkaTemplate.send(
                removeLinkTopic,
                RemoveLinkMessageAvro.newBuilder().setChatId(chatId).setUrl(url).build());
        throw new ScrapperClientException("На стороне сервера проблемы, но вашу ссылку мы поставили в очередь");
    }

    private void handleScrapperClientException(ClientHttpResponse response) throws IOException {
        String description = "Ошибка клиента: %s".formatted(response.getStatusText());

        log.atError()
                .setMessage("Бизнес-ошибка")
                .addKeyValue("message", description)
                .log();
        throw new ScrapperClientException(description);
    }
}

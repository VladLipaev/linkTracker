package backend.academy.linktracker.scrapper.controller.kafka;

import backend.academy.linktracker.bot.dto.avro.AddLinkMessageAvro;
import backend.academy.linktracker.scrapper.config.metrics.ScrapperMetrics;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.service.ScrapperLinksService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Controller;
import java.net.URI;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AddLinkConsumer {

    private final ScrapperLinksService linksService;
    private final ScrapperMetrics metrics;

    @KafkaListener(topics = "${app.kafka.consumers.topic.link-add}")
    public void listen(AddLinkMessageAvro addLinkMessageAvro) {
        long start = System.currentTimeMillis();
        long chatId = addLinkMessageAvro.getChatId();
        AddLinkRequest addLinkRequest = AddLinkRequest.builder()
            .link(URI.create(addLinkMessageAvro.getUrl()))
            .tags(addLinkMessageAvro.getTags())
            .build();

        try {
            linksService.addLink(chatId, addLinkRequest);

            log.atDebug()
                    .setMessage("добавление ссылки принято")
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("url", addLinkRequest.getLink())
                    .log();
        } catch (DataAccessException e) {
            log.atError()
                    .setMessage("Ошибка обработки добавления ссылки: нет доступа к бд")
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("error", e.getMessage())
                    .log();
            throw new RetryableException(e);
        } catch (Exception e) {
            log.atError()
                    .setMessage("Ошибка обработки добавления ссылки")
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("error", e.getMessage())
                    .log();
            throw e;
        } finally {
            metrics.recordRequestDuration(System.currentTimeMillis() - start, "kafka", "consumer");
        }
    }
}

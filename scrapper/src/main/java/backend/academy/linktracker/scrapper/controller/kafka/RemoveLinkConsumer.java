package backend.academy.linktracker.scrapper.controller.kafka;

import backend.academy.linktracker.bot.dto.avro.RemoveLinkMessageAvro;
import backend.academy.linktracker.scrapper.config.metrics.ScrapperMetrics;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
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
public class RemoveLinkConsumer {

    private final ScrapperLinksService linksService;
    private final ScrapperMetrics metrics;

    @KafkaListener(topics = "${app.kafka.consumers.topic.link-remove}")
    public void listen(RemoveLinkMessageAvro removeLinkMessageAvro) {
        long start = System.currentTimeMillis();
        long chatId = removeLinkMessageAvro.getChatId();
        RemoveLinkRequest removeLinkRequest = RemoveLinkRequest.builder()
            .link(URI.create(removeLinkMessageAvro.getUrl()))
            .build();

        try {
            linksService.removeLink(chatId, removeLinkRequest);
            log.atDebug()
                    .setMessage("удаление ссылки принято")
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("url", removeLinkRequest.getLink())
                    .log();
        } catch (DataAccessException e) {
            log.atError()
                    .setMessage("Ошибка обработки удаления ссылки: нет доступа к бд")
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("error", e.getMessage())
                    .log();
            throw new RetryableException(e);
        } catch (Exception e) {
            log.atError()
                    .setMessage("Ошибка обработки удаления ссылки")
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("error", e.getMessage())
                    .log();
            throw e;
        } finally {
            metrics.recordRequestDuration(System.currentTimeMillis() - start, "kafka", "consumer");
        }
    }
}

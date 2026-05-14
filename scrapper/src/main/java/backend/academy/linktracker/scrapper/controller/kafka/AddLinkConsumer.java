package backend.academy.linktracker.scrapper.controller.kafka;

import backend.academy.linktracker.bot.dto.avro.AddLinkMessageAvro;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.service.ScrapperLinksService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AddLinkConsumer {

    private final ScrapperLinksService linksService;

    @KafkaListener(topics = "${app.kafka.consumers.topic.link-add}")
    public void listen(AddLinkMessageAvro addLinkMessageAvro) {
        long chatId = addLinkMessageAvro.getChatId();
        AddLinkRequest addLinkRequest = new AddLinkRequest(addLinkMessageAvro.getUrl(), addLinkMessageAvro.getTags());

        try {
            linksService.addLink(chatId, addLinkRequest);

            log.atDebug()
                    .setMessage("добавление ссылки принято")
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("url", addLinkRequest.link())
                    .log();
        } catch (DataAccessException e) {
            log.atError()
                    .setMessage("Ошибка обработки сообщения: нет доступа к бд")
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("error", e.getMessage())
                    .log();
            throw new RetryableException(e);
        } catch (Exception e) {
            log.atError()
                    .setMessage("Ошибка обработки сообщения")
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("error", e.getMessage())
                    .log();
            throw e;
        }
    }
}

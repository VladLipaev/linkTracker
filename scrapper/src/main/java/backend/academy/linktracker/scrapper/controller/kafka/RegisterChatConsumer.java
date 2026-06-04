package backend.academy.linktracker.scrapper.controller.kafka;

import backend.academy.linktracker.bot.dto.avro.RegisterChatAvro;
import backend.academy.linktracker.scrapper.config.metrics.ScrapperMetrics;
import backend.academy.linktracker.scrapper.service.TgChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class RegisterChatConsumer {

    private final TgChatService tgChatService;
    private final ScrapperMetrics metrics;

    @KafkaListener(topics = "${app.kafka.consumers.topic.chat-reg}")
    public void listen(RegisterChatAvro registerChatAvro) {
        long start = System.currentTimeMillis();
        long chatId = registerChatAvro.getChatId();

        try {
            tgChatService.addTgChat(chatId);

            log.atDebug()
                    .setMessage("регистрация чата принята")
                    .addKeyValue("chatId", chatId)
                    .log();
        } catch (DataAccessException e) {
            log.atError()
                    .setMessage("Ошибка обработки регистрации чата: нет доступа к бд")
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
        } finally {
            metrics.recordRequestDuration(System.currentTimeMillis() - start, "kafka", "consumer");
        }
    }
}

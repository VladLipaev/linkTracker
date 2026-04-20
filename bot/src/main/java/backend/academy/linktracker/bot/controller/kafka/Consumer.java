package backend.academy.linktracker.bot.controller.kafka;

import backend.academy.linktracker.bot.controller.kafka.exception.RetryableException;
import backend.academy.linktracker.bot.controller.kafka.service.IdempotencyService;
import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.bot.service.TelegramUpdateService;
import backend.academy.linktracker.scrapper.dto.avro.LinkUpdateAvro;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.ResourceAccessException;

@Controller
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "app.communication.controller",
        name = "mode",
        havingValue = "kafka",
        matchIfMissing = true)
public class Consumer {

    private final TelegramUpdateService telegramUpdateService;
    private final IdempotencyService idempotencyService;
    private final Validator validator;

    @KafkaListener(topics = "link-updates-topic")
    public void listen(LinkUpdateAvro linkUpdateAvro, @Header("event-id") byte[] eventIdBytes) {
        LinkUpdate linkUpdate = avroToLinkUpdate(linkUpdateAvro);
        Set<ConstraintViolation<LinkUpdate>> violations = validator.validate(linkUpdate);
        if (!violations.isEmpty()) {
            log.atError()
                    .setMessage("ошибка валидации кафка сообщения")
                    .addKeyValue(
                            "violations",
                            violations.stream()
                                    .map(ConstraintViolation::getMessage)
                                    .toList())
                    .log();
            throw new ConstraintViolationException(violations);
        }
        String eventIdString = new String(eventIdBytes, StandardCharsets.UTF_8);
        UUID eventId = UUID.fromString(eventIdString);

        if (idempotencyService.exists(eventId)) {
            log.atWarn()
                    .setMessage("данное сообщение уже было обработано")
                    .addKeyValue("message.id", eventId)
                    .log();
            return;
        }
        try {
            telegramUpdateService.postUpdate(linkUpdate);
            idempotencyService.markAsProcessed(eventId);
            log.info("Уведомление было отправлено пользователям");
        } catch (ResourceAccessException e) {
            log.atWarn()
                    .setMessage("Соединение с тг прервано, повторная отправка сообщения...")
                    .setCause(e)
                    .log();
            throw new RetryableException(e);
        }
    }

    private LinkUpdate avroToLinkUpdate(LinkUpdateAvro linkUpdateAvro) {

        return new LinkUpdate(
                linkUpdateAvro.getId(),
                linkUpdateAvro.getUrl().toString(),
                linkUpdateAvro.getDescription().toString(),
                linkUpdateAvro.getTgChatIds());
    }
}

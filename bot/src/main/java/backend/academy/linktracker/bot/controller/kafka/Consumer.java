package backend.academy.linktracker.bot.controller.kafka;

import backend.academy.linktracker.bot.controller.kafka.exception.RetryableException;
import backend.academy.linktracker.bot.controller.kafka.service.IdempotencyService;
import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.bot.service.TelegramUpdateService;
import backend.academy.linktracker.scrapper.dto.avro.ProcessedLinkUpdateAvro;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.ResourceAccessException;

@Controller
@RequiredArgsConstructor
@Slf4j
public class Consumer {

    private final TelegramUpdateService telegramUpdateService;
    private final IdempotencyService idempotencyService;
    private final Validator validator;
    private final AvroToLinkUpdate avroToLinkUpdate;

    @KafkaListener(topics = "${app.kafka.topic.name:link.processed-updates}")
    public void listen(
            ProcessedLinkUpdateAvro linkUpdateAvro, @Header(name = "event-id", required = false) byte[] eventIdBytes) {
        if (eventIdBytes == null) {
            log.atError().setMessage("event-id не был указан").log();
            throw new NullPointerException("event-id не был указан");
        }
        LinkUpdate linkUpdate = avroToLinkUpdate.avroToLinkUpdate(linkUpdateAvro);
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
        if (!idempotencyService.tryLock(eventId)) {
            log.atWarn()
                    .setMessage("данное сообщение уже обрабатывается или было обработано")
                    .addKeyValue("message.id", eventId)
                    .log();
            return;
        }
        try {
            telegramUpdateService.postUpdate(linkUpdate);
            log.info("Уведомление было отправлено пользователям");
        } catch (ResourceAccessException e) {
            log.atWarn()
                    .setMessage("Соединение с тг прервано, повторная отправка сообщения...")
                    .setCause(e)
                    .log();
            idempotencyService.removeEvent(eventId);
            throw new RetryableException(e);
        }
    }
}

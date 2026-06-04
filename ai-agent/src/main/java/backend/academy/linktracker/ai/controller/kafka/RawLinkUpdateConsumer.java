package backend.academy.linktracker.ai.controller.kafka;

import backend.academy.linktracker.ai.entity.dto.ProcessedLinkUpdateDto;
import backend.academy.linktracker.ai.service.AiAgentIdempotencyService;
import backend.academy.linktracker.ai.service.PrioritizationService;
import backend.academy.linktracker.ai.service.ProcessedLinkUpdateService;
import backend.academy.linktracker.ai.service.SummarizeService;
import backend.academy.linktracker.scrapper.dto.avro.RawLinkUpdateAvro;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class RawLinkUpdateConsumer {

    private final RawLinkUpdateValidator rawLinkUpdateValidator;
    private final SummarizeService summarizeService;
    private final PrioritizationService prioritizationService;
    private final AiAgentIdempotencyService idempotencyService;
    private final ProcessedLinkUpdateService processedLinkUpdateService;

    @KafkaListener(topics = "${app.kafka.consumer.topic.name}")
    public void listen(
            RawLinkUpdateAvro rawLinkUpdateAvro, @Header(name = "event-id", required = false) byte[] eventIdBytes) {
        if (eventIdBytes == null) {
            log.atError().setMessage("event-id не был указан").log();
            throw new IllegalArgumentException("event-id не был указан");
        }
        boolean validate = rawLinkUpdateValidator.validate(rawLinkUpdateAvro);
        if (!validate) {
            log.atError()
                    .setMessage("Сообщение не прошло валидацию")
                    .addKeyValue("message.id", rawLinkUpdateAvro.getId())
                    .log();
            throw new RawLinkUpdateValidationException("Сообщение не прошло валидацию");
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
        String url = extractUrl(rawLinkUpdateAvro.getDescription());
        ProcessedLinkUpdateDto processedLinkUpdateDto = prioritizationService.prioritize(rawLinkUpdateAvro);
        if (rawLinkUpdateValidator.isAboveThreshold(rawLinkUpdateAvro)) {
            processedLinkUpdateDto.setDescription(
                    String.format("%s%n%s", url, summarizeService.summarize(processedLinkUpdateDto.getDescription())));
        }
        processedLinkUpdateService.saveProcessedLinkUpdate(processedLinkUpdateDto);
    }

    private String extractUrl(String description) {
        int newlineIndex = description.indexOf('\n');
        if (newlineIndex != -1) {
            return description.substring(0, newlineIndex).trim();
        }
        return "unknown url";
    }
}

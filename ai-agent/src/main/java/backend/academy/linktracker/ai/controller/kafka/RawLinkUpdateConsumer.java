package backend.academy.linktracker.ai.controller.kafka;

import backend.academy.linktracker.ai.dto.LinkUpdate;
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
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Controller;
import tools.jackson.databind.ObjectMapper;

@Controller
@RequiredArgsConstructor
@Slf4j
public class RawLinkUpdateConsumer {

    private final RawLinkUpdateValidator rawLinkUpdateValidator;
    private final SummarizeService summarizeService;
    private final PrioritizationService prioritizationService;
    private final AiAgentIdempotencyService idempotencyService;
    private final ProcessedLinkUpdateService processedLinkUpdateService;
    private final ObjectMapper objectMapper;
    private final LinkUpdateToAvroMapper linkUpdateToAvroMapper;

    @KafkaListener(topics = "${app.kafka.consumer.topic.name}")
    public void listen(
        LinkUpdate linkUpdate,
        @Header(name = "event-id", required = false) byte[] eventIdBytes,
        Acknowledgment acknowledgment) {

        if (eventIdBytes == null) {
            log.atError().setMessage("event-id не был указан").log();
            throw new IllegalArgumentException("event-id не был указан");
        }

        RawLinkUpdateAvro rawLinkUpdateAvro = linkUpdateToAvroMapper.rawLinkUpdateAvro(linkUpdate);
        if (!rawLinkUpdateValidator.validate(rawLinkUpdateAvro)) {
            log.atError()
                .setMessage("Сообщение не прошло валидацию")
                .addKeyValue("message.id", rawLinkUpdateAvro.getId())
                .log();
            throw new RawLinkUpdateValidationException("Сообщение не прошло валидацию");
        }

        UUID eventId = UUID.fromString(new String(eventIdBytes, StandardCharsets.UTF_8));

        if (!idempotencyService.tryLock(eventId)) {
            log.atWarn()
                .setMessage("данное сообщение уже обрабатывается или было обработано")
                .addKeyValue("message.id", eventId)
                .log();
            acknowledgment.acknowledge();
            return;
        }

        try {
            String url = extractUrl(rawLinkUpdateAvro.getDescription());
            ProcessedLinkUpdateDto processedLinkUpdateDto = prioritizationService.prioritize(rawLinkUpdateAvro);
            if (rawLinkUpdateValidator.isAboveThreshold(rawLinkUpdateAvro)) {
//                processedLinkUpdateDto.setDescription(
//                    String.format("%s%n%s", url, summarizeService.summarize(processedLinkUpdateDto.getDescription())));
                processedLinkUpdateDto.setDescription(String.format("%s%n%s", url, "..."));
            }
            processedLinkUpdateService.saveProcessedLinkUpdate(processedLinkUpdateDto);

            acknowledgment.acknowledge();
            log.info("сообщение с event-id: {}, url: {}, обработано", eventId, linkUpdate.url());
        } catch (Exception e) {
            idempotencyService.release(eventId);
            throw e;
        }
    }

    private String extractUrl(String description) {
        int newlineIndex = description.indexOf('\n');
        if (newlineIndex != -1) {
            return description.substring(0, newlineIndex).trim();
        }
        return "unknown url";
    }
}

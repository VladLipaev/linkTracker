package backend.academy.linktracker.ai.controller.kafka;

import backend.academy.linktracker.ai.service.SummarizeService;
import backend.academy.linktracker.scrapper.dto.avro.RawLinkUpdateAvro;
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

    @KafkaListener(topics = "${app.kafka.consumer.topic.name}")
    public void listen(
            RawLinkUpdateAvro rawLinkUpdateAvro, @Header(name = "event-id", required = false) byte[] eventIdBytes) {
        if (eventIdBytes == null) {
            log.atError().setMessage("event-id не был указан").log();
            throw new IllegalArgumentException("event-id не был указан");
        }
        boolean validate = rawLinkUpdateValidator.validate(rawLinkUpdateAvro);
        if (validate && rawLinkUpdateValidator.isAboveThreshold(rawLinkUpdateAvro)) {
            rawLinkUpdateAvro.setDescription(summarizeService.summarize(rawLinkUpdateAvro.getDescription()));
            log.atInfo()
                    .setMessage(rawLinkUpdateAvro.getDescription())
                    .addKeyValue("id", rawLinkUpdateAvro.getId())
                    .log();
        }
    }
}

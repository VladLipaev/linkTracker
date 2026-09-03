package backend.academy.linktracker.scrapper.service.kafka;

import backend.academy.linktracker.scrapper.config.metrics.DebeziumMetrics;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.dto.avro.RawLinkUpdateAvro;
import backend.academy.linktracker.scrapper.entity.OutBoxMessage;
import backend.academy.linktracker.scrapper.repository.outbox.OutBoxRepository;
import backend.academy.linktracker.scrapper.service.NotificationUpdateSender;
import io.micrometer.tracing.Tracer;
import io.opentelemetry.api.trace.Span;
import jakarta.annotation.Priority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Priority(2)
@Slf4j
public class KafkaNotificationUpdateSender implements NotificationUpdateSender {

    private final OutBoxRepository outBoxRepository;
    private final DebeziumMetrics debeziumMetrics;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendUpdate(LinkUpdate update) {

        String traceId = Span.current().getSpanContext().getTraceId();
        String spanId = Span.current().getSpanContext().getSpanId();

        OutBoxMessage outBoxMessage = OutBoxMessage.builder()
            .payload(update)
            .partitionKey(String.valueOf(update.id()))
            .traceId(traceId)
            .spanId(spanId)
            .aggregateId(update.id().toString())
            .aggregateType("LinkUpdate")
            .eventType("LinkUpdate")
            .build();
        outBoxRepository.save(outBoxMessage);
        debeziumMetrics.incrementOutboxCreated();
        log.atInfo()
                .setMessage("Уведомление отправлено в outbox")
                .addKeyValue("url", update.url())
                .log();
    }
}

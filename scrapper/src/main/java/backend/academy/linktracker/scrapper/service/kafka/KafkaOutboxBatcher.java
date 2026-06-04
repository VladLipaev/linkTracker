package backend.academy.linktracker.scrapper.service.kafka;

import backend.academy.linktracker.scrapper.config.metrics.ScrapperMetrics;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.dto.avro.RawLinkUpdateAvro;
import backend.academy.linktracker.scrapper.entity.OutBoxMessage;
import backend.academy.linktracker.scrapper.repository.outbox.OutBoxRepository;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaOutboxBatcher {

    private final OutBoxRepository outBoxRepository;
    private final KafkaTemplate<String, RawLinkUpdateAvro> kafkaTemplate;
    private final NewTopic newTopic;
    private final ObjectMapper objectMapper;
    private final LinkUpdateToAvroMapper mapper;
    private final ScrapperMetrics metrics;

    @Value("${app.kafka.outbox.batch-send-size:500}")
    private Integer BATCH_SEND_SIZE;

    @Value("${app.kafka.outbox.batch-clean-size:5000}")
    private Integer BATCH_CLEAN_SIZE;

    @Value("${app.kafka.outbox.max-retries:5}")
    private Integer maxRetries;

    @Transactional
    public int sendBatchToKafka() {
        List<OutBoxMessage> messages = outBoxRepository.findNewWithLock(BATCH_SEND_SIZE, maxRetries);
        if (messages.isEmpty()) return 0;

        List<CompletableFuture<?>> futures = new ArrayList<>();
        Map<UUID, Throwable> results = new ConcurrentHashMap<>();

        for (OutBoxMessage message : messages) {
            LinkUpdate linkUpdate = objectMapper.readValue(message.getPayload(), LinkUpdate.class);
            RawLinkUpdateAvro rawLinkUpdateAvro = mapper.rawLinkUpdateAvro(linkUpdate);
            ProducerRecord<String, RawLinkUpdateAvro> record =
                    new ProducerRecord<>(newTopic.name(), message.getPartitionKey(), rawLinkUpdateAvro);
            record.headers().add("event-id", message.getId().toString().getBytes(StandardCharsets.UTF_8));
            long start = System.currentTimeMillis();
            CompletableFuture<SendResult<String, RawLinkUpdateAvro>> future = kafkaTemplate.send(record);
            future.whenComplete((result, ex) -> {
                metrics.recordRequestDuration(System.currentTimeMillis() - start, "kafka", "producer");
                if (ex != null) {
                    results.put(message.getId(), ex);
                }
            });
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        for (OutBoxMessage message : messages) {
            Throwable ex = results.get(message.getId());
            if (ex == null) {
                message.setStatus("sent");
            } else {
                message.setStatus("error");
                message.setRetryCount(message.getRetryCount() + 1);
                log.atError()
                        .setMessage("не удалось отправить сообщение")
                        .setCause(ex)
                        .addKeyValue("message.id", message.getId())
                        .log();
            }
            message.setProcessedAt(OffsetDateTime.now());
        }

        return messages.size();
    }

    @Transactional
    public int cleanUp(OffsetDateTime threshold) {
        return outBoxRepository.cleanUpBatch(threshold, BATCH_CLEAN_SIZE);
    }
}

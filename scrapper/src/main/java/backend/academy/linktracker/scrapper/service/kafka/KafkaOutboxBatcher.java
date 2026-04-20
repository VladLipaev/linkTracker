package backend.academy.linktracker.scrapper.service.kafka;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.dto.avro.LinkUpdateAvro;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.communication.client", name = "mode", havingValue = "kafka", matchIfMissing = true)
public class KafkaOutboxBatcher {

    private final OutBoxRepository outBoxRepository;
    private final KafkaTemplate<String, LinkUpdateAvro> kafkaTemplate;
    private final NewTopic newTopic;
    private final ObjectMapper objectMapper;
    private static final Integer BATCH_SEND_SIZE = 500;
    private static final Integer BATCH_CLEAN_SIZE = 5000;

    @Transactional
    public int sendBatchToKafka() {
        List<OutBoxMessage> messages = outBoxRepository.findNewWithLock(BATCH_SEND_SIZE);
        if (messages.isEmpty()) return 0;

        List<CompletableFuture<?>> futures = new ArrayList<>();
        Map<UUID, Throwable> results = new ConcurrentHashMap<>();

        for (OutBoxMessage message : messages) {
            LinkUpdate payload = objectMapper.readValue(message.getPayload(), LinkUpdate.class);
            LinkUpdateAvro linkUpdateAvro = LinkUpdateAvro.newBuilder()
                    .setId(payload.id())
                    .setDescription(payload.description())
                    .setUrl(payload.url())
                    .setTgChatIds(payload.tgChatIds())
                    .build();
            ProducerRecord<String, LinkUpdateAvro> record =
                    new ProducerRecord<>(newTopic.name(), message.getPartitionKey(), linkUpdateAvro);
            record.headers().add("event-id", message.getId().toString().getBytes(StandardCharsets.UTF_8));
            CompletableFuture<SendResult<String, LinkUpdateAvro>> future = kafkaTemplate.send(record);
            future.whenComplete((result, ex) -> {
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

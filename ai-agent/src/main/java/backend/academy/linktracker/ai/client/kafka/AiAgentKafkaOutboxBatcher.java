package backend.academy.linktracker.ai.client.kafka;

import backend.academy.linktracker.ai.entity.OutboxProcessedMessage;
import backend.academy.linktracker.ai.entity.dto.ProcessedLinkUpdateDto;
import backend.academy.linktracker.ai.repository.OutboxRepository;
import backend.academy.linktracker.scrapper.dto.avro.ProcessedLinkUpdateAvro;
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
public class AiAgentKafkaOutboxBatcher {

    private final OutboxRepository outBoxRepository;
    private final KafkaTemplate<String, ProcessedLinkUpdateAvro> kafkaTemplate;
    private final NewTopic newTopic;
    private final ObjectMapper objectMapper;
    private final ProcessedLinkUpdateToAvroMapper mapper;

    @Value("${app.kafka.producer.outbox.batch-send-size:500}")
    private Integer BATCH_SEND_SIZE;

    @Value("${app.kafka.producer.outbox.batch-clean-size:5000}")
    private Integer BATCH_CLEAN_SIZE;

    @Value("${app.kafka.producer.outbox.max-retries:5}")
    private Integer maxRetries;

    @Transactional
    public int sendBatchToKafka() {
        List<OutboxProcessedMessage> messages = outBoxRepository.findNewWithLock(BATCH_SEND_SIZE, maxRetries);
        if (messages.isEmpty()) return 0;

        List<CompletableFuture<?>> futures = new ArrayList<>();
        Map<UUID, Throwable> results = new ConcurrentHashMap<>();
        for (OutboxProcessedMessage message : messages) {
            ProcessedLinkUpdateDto processedLinkUpdateDto =
                    objectMapper.readValue(message.getPayload(), ProcessedLinkUpdateDto.class);
            ProcessedLinkUpdateAvro processedLinkUpdateAvro = mapper.processedLinkUpdateAvro(processedLinkUpdateDto);
            ProducerRecord<String, ProcessedLinkUpdateAvro> record = new ProducerRecord<>(
                    newTopic.name(),
                    processedLinkUpdateAvro.getTgChatIds().getFirst().toString(),
                    processedLinkUpdateAvro);
            record.headers().add("event-id", message.getId().toString().getBytes(StandardCharsets.UTF_8));
            CompletableFuture<SendResult<String, ProcessedLinkUpdateAvro>> future = kafkaTemplate.send(record);
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    results.put(message.getId(), ex);
                }
            });
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        for (OutboxProcessedMessage message : messages) {
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

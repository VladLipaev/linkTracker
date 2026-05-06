package backend.academy.linktracker.scrapper.service.kafka;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.communication.client", name = "mode", havingValue = "kafka", matchIfMissing = true)
public class KafkaOutboxWorker {

    private final KafkaOutboxBatcher kafkaOutboxBatcher;
    @Value("${app.kafka.outbox.max-batches:10}")
    private Integer maxBatchesPerRun;

    @Scheduled(fixedDelayString = "${app.kafka.send-delay:60s}")
    public void sendToKafka() {
        int batchesProcessed = 0;

        try {
            while (batchesProcessed < maxBatchesPerRun) {
                int processedCount = kafkaOutboxBatcher.sendBatchToKafka();
                if (processedCount == 0) {
                    break;
                }
                batchesProcessed++;
            }

            if (batchesProcessed > 0) {
                log.atInfo()
                    .setMessage("Батч-отправка завершена")
                    .addKeyValue("batches.processed", batchesProcessed)
                    .log();
            }
        } catch (Exception e) {
            log.atError()
                .setMessage("Ошибка при обработке Outbox-батча. Прерывание цикла до следующего запуска планировщика.")
                .setCause(e)
                .log();
        }
    }

    @Scheduled(fixedDelayString = "${app.kafka.outbox.delay:120s}")
    public void refreshOutboxTable() {
        OffsetDateTime threshold = OffsetDateTime.now().minusHours(1);
        int deletedCount;
        int totalCount = 0;
        do {
            deletedCount = kafkaOutboxBatcher.cleanUp(threshold);
            totalCount += deletedCount;

        } while (deletedCount > 0);
        log.atInfo()
                .setMessage("очистка outbox таблицы завершена")
                .addKeyValue("size", totalCount)
                .log();
    }
}

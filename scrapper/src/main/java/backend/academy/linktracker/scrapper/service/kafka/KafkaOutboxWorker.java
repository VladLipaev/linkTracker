package backend.academy.linktracker.scrapper.service.kafka;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.communication.client", name = "mode", havingValue = "kafka", matchIfMissing = true)
public class KafkaOutboxWorker {

    private final KafkaOutboxBatcher kafkaOutboxBatcher;

    @Scheduled(fixedDelayString = "60s")
    public void sendToKafka() {
        while (true) {
            int processedCount = kafkaOutboxBatcher.sendBatchToKafka();
            if (processedCount == 0) break;
        }
        log.atInfo().setMessage("батч отправка сообщений закончена").log();
    }

    @Scheduled(fixedDelayString = "120s")
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

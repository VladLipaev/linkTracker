package backend.academy.linktracker.bot.controller.kafka;

import backend.academy.linktracker.bot.controller.kafka.service.IdempotencyService;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaIdempotencyScheduler {

    private final IdempotencyService idempotencyService;

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    public void cleanUp() {
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(1);

        idempotencyService.deleteOldEvents(threshold);

        log.info("Очистка таблицы идемпотентности завершена");
    }
}

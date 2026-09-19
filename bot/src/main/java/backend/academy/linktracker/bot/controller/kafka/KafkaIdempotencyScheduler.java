package backend.academy.linktracker.bot.controller.kafka;

import backend.academy.linktracker.bot.controller.kafka.service.IdempotencyService;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaIdempotencyScheduler {

    private final IdempotencyService idempotencyService;

    @Scheduled(cron = "${app.scheduled.cron.KafkaIdempotencyScheduler_cleanUp}")
    @SchedulerLock(name = "KafkaIdempotencyScheduler_cleanUp",
        lockAtMostFor = "${app.schedulerLock.lockAtMostFor.KafkaIdempotencyScheduler_cleanUp:10m}",
        lockAtLeastFor = "${app.schedulerLock.lockAtLeastFor.KafkaIdempotencyScheduler_cleanUp:3m}")
    public void cleanUp() {
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(1);

        idempotencyService.deleteOldEvents(threshold);

        log.info("Очистка таблицы идемпотентности завершена");
    }
}

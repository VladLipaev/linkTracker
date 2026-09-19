package backend.academy.linktracker.ai.scheduler;

import backend.academy.linktracker.ai.service.AiAgentIdempotencyService;
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
public class AIAgentKafkaIdempotencyScheduler {

    private final AiAgentIdempotencyService idempotencyService;

    @Scheduled(cron = "${app.scheduled.cron.AIAgentKafkaIdempotencyScheduler_cleanUp}")
    @SchedulerLock(name = "AIAgentKafkaIdempotencyScheduler_cleanUp",
        lockAtLeastFor = "${app.schedulerLock.lockAtLeastFor.AIAgentKafkaIdempotencyScheduler_cleanUp:3m}",
        lockAtMostFor = "${app.schedulerLock.lockAtMostFor.AIAgentKafkaIdempotencyScheduler_cleanUp:10m}")
    public void cleanUp() {
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(1);

        idempotencyService.deleteOldEvents(threshold);

        log.info("Очистка таблицы идемпотентности завершена");
    }
}

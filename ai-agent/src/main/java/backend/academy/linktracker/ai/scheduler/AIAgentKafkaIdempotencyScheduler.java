package backend.academy.linktracker.ai.scheduler;

import backend.academy.linktracker.ai.service.AiAgentIdempotencyService;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AIAgentKafkaIdempotencyScheduler {

    private final AiAgentIdempotencyService idempotencyService;

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    public void cleanUp() {
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(1);

        idempotencyService.deleteOldEvents(threshold);

        log.info("Очистка таблицы идемпотентности завершена");
    }
}

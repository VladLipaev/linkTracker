package backend.academy.linktracker.bot.schedule;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.bot.AbstractIntegrationTest;
import backend.academy.linktracker.bot.controller.kafka.KafkaIdempotencyScheduler;
import backend.academy.linktracker.bot.controller.kafka.service.IdempotencyService;
import java.time.OffsetDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


class SchedulerLockIT extends AbstractIntegrationTest {

    @Autowired
    private KafkaIdempotencyScheduler kafkaIdempotencyScheduler;

    @MockitoBean
    private IdempotencyService idempotencyService;

    @Test
    void shouldExecuteOnlyOnceWhenTwoInstancesTryToRun() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        Runnable runnable = () -> {
            try {
                latch.await();
                kafkaIdempotencyScheduler.cleanUp(); // метод с @SchedulerLock, вызываем вручную
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        executor.submit(runnable);
        executor.submit(runnable);
        latch.countDown();

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        verify(idempotencyService, times(1)).deleteOldEvents(any(OffsetDateTime.class));
    }
}

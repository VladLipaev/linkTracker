package backend.academy.linktracker.ai.scheduler;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.ai.AbstractIntegrationTest;
import backend.academy.linktracker.ai.client.YandexGPTRestClient;
import backend.academy.linktracker.ai.client.kafka.AiAgentKafkaOutboxBatcher;
import backend.academy.linktracker.ai.client.kafka.AiAgentKafkaOutboxWorker;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import backend.academy.linktracker.ai.controller.kafka.RawLinkUpdateConsumer;
import backend.academy.linktracker.ai.service.SummarizeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


class SchedulerLockIT extends AbstractIntegrationTest {

    @Autowired
    private AiAgentKafkaOutboxWorker kafkaOutboxWorker;

    @MockitoBean
    private AiAgentKafkaOutboxBatcher batcher;

    @MockitoBean
    private SummarizeService summarizeService;

    @MockitoBean
    private YandexGPTRestClient yandexGPTRestClient;

    @MockitoBean
    private RawLinkUpdateConsumer rawLinkUpdateConsumer;

    @Test
    void shouldExecuteOnlyOnceWhenTwoInstancesTryToRun() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        Runnable runnable = () -> {
            try {
                latch.await();
                kafkaOutboxWorker.sendToKafka(); // метод с @SchedulerLock, вызываем вручную
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        executor.submit(runnable);
        executor.submit(runnable);
        latch.countDown();

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        when(batcher.sendBatchToKafka()).thenReturn(0);
        verify(batcher, times(1)).sendBatchToKafka();
    }
}

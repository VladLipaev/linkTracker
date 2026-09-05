//package backend.academy.linktracker.scrapper.schedule;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//import org.testcontainers.postgresql.PostgreSQLContainer;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//@SpringBootTest
//@Testcontainers
//class SchedulerLockIntegrationTest {
//
//    @Container
//    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");
//
//    @Autowired
//    private MyScheduledTask task;
//
//    @Test
//    void shouldExecuteOnlyOnceWhenTwoInstancesTryToRun() throws Exception {
//        AtomicInteger counter = new AtomicInteger(0);
//        task.setCounter(counter); // или использовать мок
//
//        ExecutorService executor = Executors.newFixedThreadPool(2);
//        CountDownLatch latch = new CountDownLatch(1);
//
//        Runnable runnable = () -> {
//            try {
//                latch.await();
//                task.scrape(); // метод с @SchedulerLock, вызываем вручную
//            } catch (InterruptedException e) {}
//        };
//
//        executor.submit(runnable);
//        executor.submit(runnable);
//        latch.countDown();
//
//        executor.shutdown();
//        executor.awaitTermination(5, TimeUnit.SECONDS);
//
//        assertEquals(1, counter.get());
//    }
//}

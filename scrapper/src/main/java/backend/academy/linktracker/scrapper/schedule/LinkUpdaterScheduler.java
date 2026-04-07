package backend.academy.linktracker.scrapper.schedule;

import backend.academy.linktracker.scrapper.entity.Link;
import backend.academy.linktracker.scrapper.repository.LinksRepository;
import backend.academy.linktracker.scrapper.service.LinkProcessorService;
import backend.academy.linktracker.scrapper.service.NotificationUpdateSender;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkUpdaterScheduler {

    private final LinksRepository linksRepository;
    private final LinkProcessorService processorService;
    private final SchedulerProperties properties;
    private final NotificationUpdateSender updateSender;

    private ExecutorService executorService;

    // Инициализируем ThreadPool с количеством потоков из конфига
    @PostConstruct
    public void init() {
        this.executorService = Executors.newFixedThreadPool(properties.threadCount());
    }

    @PreDestroy
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Scheduled(fixedDelayString = "${app.scheduler.interval}")
    public void update() {
        log.info("Начинаем батч-проверку ссылок...");

        List<Link> batch = linksRepository.findLinksToCheck(properties.batchSize());

        if (batch.isEmpty()) {
            log.debug("Нет ссылок для проверки.");
            return;
        }

        int chunkSize = (int) Math.ceil((double) batch.size() / properties.threadCount());
        List<List<Link>> chunks = partitionList(batch, chunkSize);

        List<CompletableFuture<Void>> futures = chunks.stream()
                .map(chunk -> CompletableFuture.runAsync(() -> processChunk(chunk), executorService))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<Long> processedIds = batch.stream().map(Link::getId).toList();
        linksRepository.updateLastCheckedAt(processedIds, OffsetDateTime.now());

        log.atInfo()
                .setMessage("Батч ссылок успешно обработан.")
                .addKeyValue("batch_size", batch.size())
                .log();
    }

    private void processChunk(List<Link> chunk) {
        for (Link link : chunk) {
            try {
                processorService
                        .processLink(link)
                        .ifPresent(result -> processorService.handleUpdateResult(result, link));
            } catch (Exception e) {
                log.atError()
                        .setMessage("Непредвиденная ошибка сервиса")
                        .setCause(e)
                        .log();
            }
        }
    }

    private <T> List<List<T>> partitionList(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(new ArrayList<>(list.subList(i, Math.min(i + size, list.size()))));
        }
        return partitions;
    }
}

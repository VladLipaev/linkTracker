package backend.academy.linktracker.ai.scheduler;

import backend.academy.linktracker.ai.entity.OutboxProcessedMessage;
import backend.academy.linktracker.ai.entity.ProcessedLinkUpdate;
import backend.academy.linktracker.ai.entity.dto.Priority;
import backend.academy.linktracker.ai.entity.dto.ProcessedLinkUpdateDto;
import backend.academy.linktracker.ai.repository.OutboxRepository;
import backend.academy.linktracker.ai.repository.ProcessedLinkUpdateRepository;
import backend.academy.linktracker.ai.service.ProcessedLinkUpdateService;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class GroupingOutboxScheduler {

    private final ProcessedLinkUpdateRepository processedRepo;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final ProcessedLinkUpdateService processedLinkUpdateService;

    @Value("${app.grouping.window-ms}")
    private long windowMs;

    @Value("${app.kafka.producer.outbox.batch-take-size}")
    private Integer batchTakeSize;

    @Scheduled(fixedDelayString = "${app.grouping.window-ms}")
    @Transactional
    public void groupAndMoveToOutbox() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime windowStart = now.minus(Duration.ofMillis(windowMs));

        List<ProcessedLinkUpdate> batch = processedRepo.findBatchWithSkipLocked(windowStart, batchTakeSize);

        if (batch.isEmpty()) {
            log.atInfo().setMessage("новых обновлений нету для группировки").log();
            return;
        }
        List<UUID> processedIds = new ArrayList<>();
        Map<Long, List<ProcessedLinkUpdate>> groupedByChat =
                batch.stream().collect(Collectors.groupingBy(ProcessedLinkUpdate::getTgChatId));

        List<OutboxProcessedMessage> outboxEntries = new ArrayList<>();
        for (Map.Entry<Long, List<ProcessedLinkUpdate>> entry : groupedByChat.entrySet()) {
            try {
                Long tgChatId = entry.getKey();
                List<ProcessedLinkUpdate> updates = entry.getValue();
                ProcessedLinkUpdateDto processedLinkUpdateDto;
                if (updates.size() == 1) {
                    processedLinkUpdateDto = new ProcessedLinkUpdateDto(
                            updates.getFirst().getOriginalEventId(),
                            updates.getFirst().getDescription(),
                            List.of(tgChatId),
                            updates.getFirst().getPriority());
                } else {
                    StringBuilder desc = new StringBuilder();
                    Priority maxPriority = Priority.LOW;
                    for (int i = 0; i < updates.size(); i++) {
                        ProcessedLinkUpdate u = updates.get(i);
                        desc.append(i + 1)
                                .append(") ")
                                .append(u.getDescription())
                                .append("\n");
                        if (u.getPriority().ordinal() > maxPriority.ordinal()) {
                            maxPriority = u.getPriority();
                        }
                    }

                    processedLinkUpdateDto = new ProcessedLinkUpdateDto(
                            updates.getFirst().getOriginalEventId(),
                            desc.toString().trim(),
                            List.of(tgChatId),
                            maxPriority);
                }

                String payloadJson = objectMapper.writeValueAsString(processedLinkUpdateDto);

                OutboxProcessedMessage outbox =
                        new OutboxProcessedMessage(UUID.randomUUID(), payloadJson, String.valueOf(tgChatId));
                outboxEntries.add(outbox);
                processedIds.addAll(
                        updates.stream().map(ProcessedLinkUpdate::getId).toList());
            } catch (Exception e) {
                log.atError()
                        .setMessage("Не удалось сформировать сообщение")
                        .setCause(e)
                        .log();
                for (ProcessedLinkUpdate linkUpdate : entry.getValue()) {
                    linkUpdate.setRetryCount(linkUpdate.getRetryCount() + 1);
                }
                processedRepo.saveAll(entry.getValue());
            }
        }
        outboxRepository.saveAll(outboxEntries);
        if (!processedIds.isEmpty()) {
            processedRepo.markProcessed(processedIds);
        }
        log.info("Сгруппировано {} чатов, создано {} записей в outbox", groupedByChat.size(), outboxEntries.size());
    }

    @Scheduled(fixedDelayString = "${app.kafka.producer.outbox.delay:120s}")
    public void refreshProcessingLinkUpdateTable() {
        OffsetDateTime threshold = OffsetDateTime.now().minusHours(1);
        int deletedCount;
        int totalCount = 0;
        do {
            deletedCount = processedLinkUpdateService.cleanUp(threshold);
            totalCount += deletedCount;

        } while (deletedCount > 0);
        log.atInfo()
                .setMessage("очистка processedLinkUpdate таблицы завершена")
                .addKeyValue("size", totalCount)
                .log();
    }
}

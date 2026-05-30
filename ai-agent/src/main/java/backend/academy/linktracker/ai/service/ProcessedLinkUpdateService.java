package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.entity.ProcessedLinkUpdate;
import backend.academy.linktracker.ai.entity.dto.ProcessedLinkUpdateDto;
import backend.academy.linktracker.ai.repository.ProcessedLinkUpdateRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcessedLinkUpdateService {

    private final ProcessedLinkUpdateRepository processedLinkUpdateRepository;

    @Value("${app.kafka.producer.outbox.batch-clean-size}")
    private Integer batchCleanSize;

    @Transactional
    public void saveProcessedLinkUpdate(ProcessedLinkUpdateDto dto) {
        List<ProcessedLinkUpdate> entities = new ArrayList<>();
        for (Long tgChat : dto.getTgChatIds()) {
            ProcessedLinkUpdate entity = new ProcessedLinkUpdate();
            entity.setOriginalEventId(dto.getId());
            entity.setDescription(dto.getDescription());
            entity.setTgChatId(tgChat);
            entity.setPriority(dto.getPriority());
            entity.setCreatedAt(OffsetDateTime.now());
            entity.setProcessed(false);
            entity.setRetryCount(0);
            entities.add(entity);
        }
        processedLinkUpdateRepository.saveAll(entities);
    }

    @Transactional
    public int cleanUp(OffsetDateTime threshold) {
        return processedLinkUpdateRepository.cleanUpBatch(threshold, batchCleanSize);
    }
}

package backend.academy.linktracker.ai.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.ai.entity.OutboxProcessedMessage;
import backend.academy.linktracker.ai.entity.ProcessedLinkUpdate;
import backend.academy.linktracker.ai.entity.dto.Priority;
import backend.academy.linktracker.ai.entity.dto.ProcessedLinkUpdateDto;
import backend.academy.linktracker.ai.repository.OutboxRepository;
import backend.academy.linktracker.ai.repository.ProcessedLinkUpdateRepository;
import backend.academy.linktracker.ai.service.ProcessedLinkUpdateService;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class GroupingOutboxSchedulerTest {

    @Mock
    private ProcessedLinkUpdateRepository processedRepo;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private ProcessedLinkUpdateService processedLinkUpdateService;

    @InjectMocks
    private GroupingOutboxScheduler scheduler;

    @Captor
    private ArgumentCaptor<List<OutboxProcessedMessage>> outboxCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "windowMs", 30000L);
        ReflectionTestUtils.setField(scheduler, "batchTakeSize", 100);
        // Внедряем реальный ObjectMapper, чтобы избежать NPE
        ReflectionTestUtils.setField(scheduler, "objectMapper", objectMapper);
    }

    @Test
    @DisplayName("TC-2.1: несколько обновлений для одного чата -> группировка")
    void shouldGroupMultipleUpdatesIntoOneOutboxMessage() throws Exception {
        Long chatId = 12345L;
        ProcessedLinkUpdate u1 = createUpdate(chatId, "First update", Priority.LOW);
        ProcessedLinkUpdate u2 = createUpdate(chatId, "Second update", Priority.HIGH);
        ProcessedLinkUpdate u3 = createUpdate(chatId, "Third update", Priority.MEDIUM);
        List<ProcessedLinkUpdate> batch = List.of(u1, u2, u3);

        when(processedRepo.findBatchWithSkipLocked(any(OffsetDateTime.class), anyInt()))
                .thenReturn(batch);
        when(outboxRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        scheduler.groupAndMoveToOutbox();

        verify(outboxRepository).saveAll(outboxCaptor.capture());
        List<OutboxProcessedMessage> outboxMessages = outboxCaptor.getValue();
        assertThat(outboxMessages).hasSize(1);

        OutboxProcessedMessage outboxMsg = outboxMessages.get(0);
        ProcessedLinkUpdateDto dto = objectMapper.readValue(outboxMsg.getPayload(), ProcessedLinkUpdateDto.class);

        assertThat(dto.getTgChatIds()).containsExactly(chatId);
        assertThat(dto.getPriority()).isEqualTo(Priority.HIGH);

        String desc = dto.getDescription();
        assertThat(desc).contains("1) First update");
        assertThat(desc).contains("2) Second update");
        assertThat(desc).contains("3) Third update");
    }

    @Test
    @DisplayName("TC-2.2: одиночное обновление -> сообщение без группировки (без нумерации)")
    void shouldNotAddNumberingForSingleUpdate() throws Exception {
        Long chatId = 67890L;
        ProcessedLinkUpdate single = createUpdate(chatId, "Single update", Priority.MEDIUM);

        when(processedRepo.findBatchWithSkipLocked(any(OffsetDateTime.class), anyInt()))
                .thenReturn(List.of(single));
        when(outboxRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        scheduler.groupAndMoveToOutbox();

        verify(outboxRepository).saveAll(outboxCaptor.capture());
        List<OutboxProcessedMessage> outboxMessages = outboxCaptor.getValue();
        assertThat(outboxMessages).hasSize(1);

        ProcessedLinkUpdateDto dto =
                objectMapper.readValue(outboxMessages.get(0).getPayload(), ProcessedLinkUpdateDto.class);
        assertThat(dto.getDescription()).isEqualTo("Single update");
    }

    private ProcessedLinkUpdate createUpdate(Long chatId, String description, Priority priority) {
        ProcessedLinkUpdate u = new ProcessedLinkUpdate();
        u.setId(UUID.randomUUID());
        u.setOriginalEventId(100L + chatId);
        u.setDescription(description);
        u.setTgChatId(chatId);
        u.setPriority(priority);
        u.setCreatedAt(OffsetDateTime.now());
        u.setProcessed(false);
        u.setRetryCount(0);
        return u;
    }
}

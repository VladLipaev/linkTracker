package backend.academy.linktracker.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.ai.config.PrioritizationProperties;
import backend.academy.linktracker.ai.entity.dto.Priority;
import backend.academy.linktracker.ai.entity.dto.ProcessedLinkUpdateDto;
import backend.academy.linktracker.scrapper.dto.avro.RawLinkUpdateAvro;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrioritizationServiceTest {

    @Mock
    private PrioritizationProperties prioritizationProperties;

    @InjectMocks
    private PrioritizationService prioritizationService;

    @BeforeEach
    void setUp() {
        when(prioritizationProperties.highKeywords()).thenReturn(List.of("critical", "urgent", "breaking", "security"));
        when(prioritizationProperties.lowKeywords()).thenReturn(List.of("minor", "typo", "chore", "docs"));
        prioritizationService.init();
    }

    @Test
    @DisplayName("TC-1.1: high keyword -> HIGH priority")
    void shouldAssignHighPriorityWhenHighKeywordPresent() {
        RawLinkUpdateAvro avro = createAvro("critical bug fix in production", 1L, List.of(123L));

        ProcessedLinkUpdateDto result = prioritizationService.prioritize(avro);

        assertThat(result.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(result.getDescription()).isEqualTo(avro.getDescription());
    }

    @Test
    @DisplayName("TC-1.2: no keywords -> MEDIUM priority")
    void shouldAssignMediumPriorityWhenNoKeywords() {
        RawLinkUpdateAvro avro = createAvro("updated documentation for API", 2L, List.of(123L));

        ProcessedLinkUpdateDto result = prioritizationService.prioritize(avro);

        assertThat(result.getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    @DisplayName("TC-1.3: low keyword -> LOW priority")
    void shouldAssignLowPriorityWhenLowKeywordPresent() {
        RawLinkUpdateAvro avro = createAvro("fix typo in readme file", 3L, List.of(123L));

        ProcessedLinkUpdateDto result = prioritizationService.prioritize(avro);

        assertThat(result.getPriority()).isEqualTo(Priority.LOW);
    }

    private RawLinkUpdateAvro createAvro(String description, long id, List<Long> chatIds) {
        return RawLinkUpdateAvro.newBuilder()
                .setId(id)
                .setDescription(description)
                .setAuthor("test-author")
                .setTgChatIds(chatIds)
                .build();
    }
}

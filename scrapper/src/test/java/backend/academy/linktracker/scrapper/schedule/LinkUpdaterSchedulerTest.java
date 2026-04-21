package backend.academy.linktracker.scrapper.schedule;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.entity.Link;
import backend.academy.linktracker.scrapper.handler.UpdateResult;
import backend.academy.linktracker.scrapper.repository.LinksRepository;
import backend.academy.linktracker.scrapper.service.HttpNotificationUpdateSender;
import backend.academy.linktracker.scrapper.service.LinkProcessorService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

@ExtendWith(MockitoExtension.class)
class LinkUpdaterSchedulerTest {

    private LinkUpdaterScheduler linkUpdaterScheduler;

    @Mock
    private LinksRepository linksRepository;

    @Mock
    private LinkProcessorService processorService;

    @Mock
    private SchedulerProperties properties;

    @Mock
    private HttpNotificationUpdateSender httpNotificationUpdateSender;

    @Mock
    private SchedulerProperties schedulerProperties;

    private final Pageable pageable = PageRequest.of(0, 10);

    @BeforeEach
    public void setUp() {
        when(properties.threadCount()).thenReturn(2);
        when(properties.batchSize()).thenReturn(10);

        linkUpdaterScheduler = new LinkUpdaterScheduler(
                linksRepository, processorService, properties, httpNotificationUpdateSender, schedulerProperties);

        linkUpdaterScheduler.init();
    }

    @Test
    public void update_whenLinksExistAndNeedUpdate_shouldProcessAndHandleResult() {
        // Arrange
        Link link1 = new Link("https://github.com/1", OffsetDateTime.now());
        link1.setId(1L);
        Link link2 = new Link("https://github.com/2", OffsetDateTime.now());
        link2.setId(2L);

        UpdateResult result = mock(UpdateResult.class);
        when(linksRepository.findLinksToCheck(pageable))
                .thenReturn(new SliceImpl<>(List.of(link1, link2), pageable, false));
        // link1 вернет обновление, link2 - нет
        when(processorService.processLink(link1)).thenReturn(Optional.of(result));
        when(processorService.processLink(link2)).thenReturn(Optional.empty());

        // Act
        linkUpdaterScheduler.update();

        // Assert
        verify(processorService).processLink(link1);
        verify(processorService).processLink(link2);

        // handleUpdateResult должен быть вызван только для того, кто вернул Optional.of
        verify(processorService, times(1)).handleUpdateResult(eq(result), eq(link1));

        // Проверяем, что в конце обновилось время проверки для всего батча
        verify(processorService).markBatchAsChecked(anyList(), any(OffsetDateTime.class));
    }

    @Test
    public void update_whenBatchIsEmpty_shouldDoNothing() {
        // Arrange
        when(linksRepository.findLinksToCheck(pageable)).thenReturn(new SliceImpl<>(List.of(), pageable, false));

        // Act
        linkUpdaterScheduler.update();

        // Assert
        verifyNoInteractions(processorService);
        verify(linksRepository, never()).updateLastCheckedAt(anyList(), any());
    }

    @Test
    public void update_shouldPartitionAndProcessAllLinksInThreads() {
        // Проверка корректности разбиения на чанки
        Link link1 = new Link("url1", OffsetDateTime.now());
        link1.setId(1L);
        Link link2 = new Link("url2", OffsetDateTime.now());
        link2.setId(2L);
        Link link3 = new Link("url3", OffsetDateTime.now());
        link3.setId(3L);

        when(linksRepository.findLinksToCheck(pageable))
                .thenReturn(new SliceImpl<>(List.of(link1, link2, link3), pageable, false));
        linkUpdaterScheduler.update();

        verify(processorService, times(3)).processLink(any(Link.class));
        verify(processorService).markBatchAsChecked(anyList(), any());
    }

    @Test
    public void update_whenInServiceThrowsException_shouldNotBreakExecution() {

        Link link1 = new Link("url1", OffsetDateTime.now());
        link1.setId(1L);
        when(linksRepository.findLinksToCheck(pageable)).thenReturn(new SliceImpl<>(List.of(link1), pageable, false));

        when(processorService.processLink(link1)).thenThrow(new IllegalArgumentException("Unexpected error"));

        assertDoesNotThrow(() -> linkUpdaterScheduler.update());
    }
}

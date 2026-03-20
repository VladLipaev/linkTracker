package backend.academy.linktracker.scrapper.schedule;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.client.bot.TelegramBotRestClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.entity.Link;
import backend.academy.linktracker.scrapper.repository.LinksRepository;
import backend.academy.linktracker.scrapper.service.LinksService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkUpdaterSchedulerTest {

    private LinkUpdaterScheduler linkUpdaterScheduler;

    @Mock
    private LinksRepository linksRepository;

    @Mock
    private TelegramBotRestClient botClient;

    @Mock
    private LinksService linksService;

    @BeforeEach
    public void setUp() {
        linkUpdaterScheduler = new LinkUpdaterScheduler(linksRepository, botClient, linksService);
    }

    @Test
    public void update_whenLinksExistAndServiceReturnsUpdate_shouldSendUpdateToBot() {
        String url = "https://github.com/user/repo";
        Link link = new Link(url, OffsetDateTime.now().minusDays(1));
        link.setId(1L);

        LinkUpdate expectedUpdate = new LinkUpdate(link.getId(), url, "Новое обновление", List.of(10L, 20L));

        when(linksRepository.findAll()).thenReturn(List.of(link));
        when(linksService.processLink(link)).thenReturn(Optional.of(expectedUpdate));

        linkUpdaterScheduler.update();

        ArgumentCaptor<LinkUpdate> updateCaptor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(botClient).sendUpdate(updateCaptor.capture());

        LinkUpdate capturedUpdate = updateCaptor.getValue();
        assertEquals(url, capturedUpdate.url());
        assertEquals(expectedUpdate.tgChatIds(), capturedUpdate.tgChatIds());

        verifyNoMoreInteractions(botClient);
    }

    @Test
    public void update_whenServiceReturnsEmpty_shouldNotSendAnything() {
        Link link = new Link("https://google.com", OffsetDateTime.now());
        when(linksRepository.findAll()).thenReturn(List.of(link));
        when(linksService.processLink(link)).thenReturn(Optional.empty());

        linkUpdaterScheduler.update();

        verifyNoInteractions(botClient);
    }

    @Test
    public void update_whenRepositoryIsEmpty_shouldSkipProcessing() {

        when(linksRepository.findAll()).thenReturn(List.of());

        linkUpdaterScheduler.update();

        verifyNoInteractions(linksService);
        verifyNoInteractions(botClient);
    }

    @Test
    public void update_whenServiceThrowsException_shouldContinueProcessingOtherLinks() {
        // Arrange
        Link failedLink = new Link("https://github.com/error", OffsetDateTime.now());
        Link successLink = new Link("https://github.com/success", OffsetDateTime.now());

        when(linksRepository.findAll()).thenReturn(List.of(failedLink, successLink));

        // Первая ссылка вызывает исключение
        when(linksService.processLink(failedLink)).thenThrow(new RuntimeException("API Error"));

        // Вторая ссылка обрабатывается успешно
        LinkUpdate update = new LinkUpdate(2L, "https://github.com/success", "Update", List.of(1L));
        when(linksService.processLink(successLink)).thenReturn(Optional.of(update));

        assertDoesNotThrow(() -> linkUpdaterScheduler.update());

        // Проверяем, что боту ушло обновление по второй ссылке
        verify(botClient).sendUpdate(update);
    }
}

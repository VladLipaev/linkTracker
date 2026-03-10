package backend.academy.linktracker.scrapper.schedule;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.client.bot.TelegramBotRestClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.entity.Link;
import backend.academy.linktracker.scrapper.handler.LinkHandler;
import backend.academy.linktracker.scrapper.repository.LinksRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Slf4j
@ExtendWith(MockitoExtension.class)
class LinkUpdaterSchedulerTest {

    private LinkUpdaterScheduler linkUpdaterScheduler;

    @Mock
    private LinksRepository linksRepository;

    @Mock
    private TelegramBotRestClient botClient;

    @Mock
    private LinkHandler linkHandler;

    @BeforeEach
    public void setUp() {
        List<LinkHandler> linkHandlers = List.of(linkHandler);
        linkUpdaterScheduler = new LinkUpdaterScheduler(linksRepository, linkHandlers, botClient);
    }

    @Test
    public void sendUpdateToChat_validUrl_shouldSendToChatsWhichHasUrl() {
        String url = "https://github.com/user/repo";
        long linkId = 1L;

        List<Long> trackingChatIds = List.of(10L, 20L);

        when(linksRepository.findAllUniqueUrls()).thenReturn(List.of(url));
        when(linkHandler.supports(url)).thenReturn(true);
        when(linksRepository.getAnyLinkByUrl(url))
                .thenReturn(Optional.of(
                        new Link(linkId, 1L, url, null, OffsetDateTime.now().minusDays(1))));

        OffsetDateTime newUpdateTime = OffsetDateTime.now();
        when(linkHandler.fetchUpdate(url)).thenReturn(newUpdateTime);

        when(linksRepository.findAllChatIdsByUrl(url)).thenReturn(trackingChatIds);

        linkUpdaterScheduler.update();

        // Перехватываем объект LinkUpdate, который планировщик попытался отправить боту
        ArgumentCaptor<LinkUpdate> updateCaptor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(botClient).sendUpdate(updateCaptor.capture());

        LinkUpdate capturedUpdate = updateCaptor.getValue();

        // Проверяем, что боту ушел правильный URL и ID
        assertEquals(url, capturedUpdate.url());
        assertEquals(linkId, capturedUpdate.id());

        // Проверяем, что в списке рассылки только чаты 10 и 20
        assertEquals(trackingChatIds, capturedUpdate.tgChatIds());
        assertEquals(2, capturedUpdate.tgChatIds().size());

        //  смотрим что боту больше не отправлялись никакие другие запросы
        verifyNoMoreInteractions(botClient);

        verify(linksRepository).updateLastUpdatedByUrl(url, newUpdateTime);
    }

    @Test
    public void update_externalApiThrowsException_shouldNotCrashAndContinueWithNextUrl() {
        // Arrange
        String failedUrl = "https://github.com/error/repo";
        String successUrl = "https://github.com/success/repo";
        long successLinkId = 2L;

        // В базе две ссылки. Первая сломается, вторая должна успешно обработаться.
        when(linksRepository.findAllUniqueUrls()).thenReturn(List.of(failedUrl, successUrl));

        when(linkHandler.supports(failedUrl)).thenReturn(true);
        when(linkHandler.supports(successUrl)).thenReturn(true);

        when(linksRepository.getAnyLinkByUrl(failedUrl))
                .thenReturn(Optional.of(
                        new Link(1L, 1L, failedUrl, null, OffsetDateTime.now().minusDays(1))));
        when(linksRepository.getAnyLinkByUrl(successUrl))
                .thenReturn(Optional.of(new Link(
                        successLinkId,
                        1L,
                        successUrl,
                        null,
                        OffsetDateTime.now().minusDays(1))));

        // Имитируем, что при запросе по первой ссылке GitHub вернул 500/404,
        // или тело ответа было кривым
        when(linkHandler.fetchUpdate(failedUrl))
                .thenThrow(new RuntimeException("GitHub API error: 500 INTERNAL_SERVER_ERROR"));

        // Вторая ссылка работает нормально
        OffsetDateTime successUpdateTime = OffsetDateTime.now();
        when(linkHandler.fetchUpdate(successUrl)).thenReturn(successUpdateTime);
        when(linksRepository.findAllChatIdsByUrl(successUrl)).thenReturn(List.of(100L));

        // Act
        // Проверяем что вызов метода не приведет к выбросу исключения
        assertDoesNotThrow(() -> linkUpdaterScheduler.update());

        // Assert
        // Проверяем что несмотря на ошибку первой ссылки вторая была успешно отправлена боту
        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(botClient).sendUpdate(captor.capture());

        assertEquals(successUrl, captor.getValue().url());
        assertEquals(successLinkId, captor.getValue().id());

        // Для сломанной ссылки обновление в БД не должно вызываться
        verify(linksRepository, never()).updateLastUpdatedByUrl(eq(failedUrl), any());
    }

    @Test
    public void update_invalidResponseBody_shouldCatchIllegalArgumentExceptionAndNotifyBot() {
        // Arrange
        String invalidUrl = "https://stackoverflow.com/questions/123456";
        long linkId = 3L;
        List<Long> chatIds = List.of(200L);

        when(linksRepository.findAllUniqueUrls()).thenReturn(List.of(invalidUrl));
        when(linkHandler.supports(invalidUrl)).thenReturn(true);
        when(linksRepository.getAnyLinkByUrl(invalidUrl))
                .thenReturn(Optional.of(new Link(
                        linkId, 1L, invalidUrl, null, OffsetDateTime.now().minusDays(1))));

        // Имитируем что StackOverflow вернул ответ без поля items
        String expectedErrorMessage = "По ссылке " + invalidUrl + " вопроса на StackOverFlow не найдено";
        when(linkHandler.fetchUpdate(invalidUrl)).thenThrow(new IllegalArgumentException(expectedErrorMessage));

        when(linksRepository.findAllChatIdsByUrl(invalidUrl)).thenReturn(chatIds);

        // Act
        assertDoesNotThrow(() -> linkUpdaterScheduler.update());

        // Assert
        // Бот должен получить уведомление об ошибке в описании
        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(botClient).sendUpdate(captor.capture());

        LinkUpdate update = captor.getValue();
        assertEquals(invalidUrl, update.url());
        assertEquals(expectedErrorMessage, update.description());
        assertEquals(chatIds, update.tgChatIds());
    }
}

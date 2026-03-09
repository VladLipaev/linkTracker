package backend.academy.linktracker.bot.handler.dialog;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.client.scrapper.ScrapperClientException;
import backend.academy.linktracker.bot.client.scrapper.ScrapperRestClient;
import backend.academy.linktracker.bot.client.telegram.TelegramClientFacade;
import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.dto.ListLinksResponse;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DialogStepProcessorTest {
    private DialogStepProcessor processor;

    private DialogManager dialogManager = new DialogManager(); // InMemory, можно настоящий

    @Mock
    private TelegramClientFacade telegramClient;

    @Mock
    private ScrapperRestClient scrapperClient;

    private BotLinkValidator validator = new BotLinkValidator();

    @BeforeEach
    void setUp() {
        dialogManager = new DialogManager();
        validator = new BotLinkValidator();
        processor = new DialogStepProcessor(dialogManager, telegramClient, scrapperClient, validator);
    }

    @Test
    void shouldRequestTagsWhenLinkIsValid() {
        // Arrange
        long chatId = 123L;
        UserSession session = new UserSession(UserState.WAITING_FOR_TRACK_LINK, null);
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        when(chat.id()).thenReturn(123L);
        when(message.text()).thenReturn("https://github.com/user/repo");
        when(message.chat()).thenReturn(chat);
        when(update.message()).thenReturn(message);
        // Act
        processor.process(update, session);

        // Assert
        verify(telegramClient).sendMessage(eq(chatId), contains("введите теги"));
        assertEquals(
                UserState.WAITING_FOR_TRACK_TAGS,
                dialogManager.getSession(chatId).state());
    }

    @Test
    void shouldReturnMessageWhenLinkIsInValid() {
        // Arrange
        long chatId = 123L;
        UserSession session = new UserSession(UserState.WAITING_FOR_TRACK_LINK, null);
        dialogManager.setSession(chatId, session);

        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        when(chat.id()).thenReturn(123L);
        when(message.text()).thenReturn("https://tbank.ru/user/repo");
        when(message.chat()).thenReturn(chat);
        when(update.message()).thenReturn(message);
        // Act
        processor.process(update, session);

        // Assert
        verify(telegramClient).sendMessage(eq(chatId), contains("Некорректная ссылка!"));
        assertEquals(
                UserState.WAITING_FOR_TRACK_LINK,
                dialogManager.getSession(chatId).state());
    }

    @Test
    void shouldReturnMessageWhenAlreadySubscribed() {
        // Arrange
        long chatId = 123L;
        String link = "https://github.com/user/repo";

        // Пользователь находится на этапе ввода тегов
        UserSession session = new UserSession(UserState.WAITING_FOR_TRACK_TAGS, link);
        dialogManager.setSession(chatId, session);

        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        when(chat.id()).thenReturn(123L);
        when(message.text()).thenReturn("skip");
        when(message.chat()).thenReturn(chat);
        when(update.message()).thenReturn(message);

        doThrow(new ScrapperClientException("Ссылка уже отслеживается"))
                .when(scrapperClient)
                .addLink(eq(chatId), eq(link), any());

        // Act
        processor.process(update, session);

        // Assert
        verify(telegramClient).sendMessage(eq(chatId), contains("Ссылка уже отслеживается"));

        // 2. Проверяем, что состояние сбросилось в BASE
        assertEquals(UserState.BASE, dialogManager.getSession(chatId).state());
    }

    @Test
    void getList_validRequest_ReturnList() {
        // Arrange
        long chatId = 123L;
        String link = "https://github.com/user/repo";

        UserSession session = new UserSession(UserState.WAITING_FOR_LIST_TAG, link);
        dialogManager.setSession(chatId, session);

        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        when(chat.id()).thenReturn(123L);
        when(message.text()).thenReturn("skip");
        when(message.chat()).thenReturn(chat);
        when(update.message()).thenReturn(message);

        doReturn(new ListLinksResponse(List.of(new LinkResponse(1L, link, null)), 1))
                .when(scrapperClient)
                .getLinks(eq(chatId), eq(null));

        // Act
        processor.process(update, session);

        // Assert
        // 1. Проверяем, что бот отправил пользователю список ссылок
        verify(telegramClient).sendMessage(eq(chatId), contains(link));

        // 2. Проверяем, что состояние сбросилось в BASE
        assertEquals(UserState.BASE, dialogManager.getSession(chatId).state());
    }

    @Test
    void getList_noLinks_ReturnMessage() {
        // Arrange
        long chatId = 123L;
        String link = "https://github.com/user/repo";

        UserSession session = new UserSession(UserState.WAITING_FOR_LIST_TAG, link);
        dialogManager.setSession(chatId, session);

        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        when(chat.id()).thenReturn(123L);
        when(message.text()).thenReturn("skip");
        when(message.chat()).thenReturn(chat);
        when(update.message()).thenReturn(message);

        doReturn(new ListLinksResponse(List.of(), 0)).when(scrapperClient).getLinks(eq(chatId), eq(null));

        // Act
        processor.process(update, session);

        // Assert
        // активных ссылок нет
        verify(telegramClient).sendMessage(eq(chatId), contains("У вас нет отслеживаемых ссылок"));

        assertEquals(UserState.BASE, dialogManager.getSession(chatId).state());
    }

    @Test
    void getListWithTag_hasLinks_ReturnListOnTag() {
        // Arrange
        long chatId = 123L;
        String link = "https://github.com/user/repo";

        UserSession session = new UserSession(UserState.WAITING_FOR_LIST_TAG, link);
        dialogManager.setSession(chatId, session);

        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        when(chat.id()).thenReturn(123L);
        when(message.text()).thenReturn("github");
        when(message.chat()).thenReturn(chat);
        when(update.message()).thenReturn(message);

        doReturn(new ListLinksResponse(List.of(new LinkResponse(1L, link, List.of("github"))), 1))
                .when(scrapperClient)
                .getLinks(eq(123L), eq("github"));

        // Act
        processor.process(update, session);

        // Assert
        verify(scrapperClient).getLinks(chatId, message.text());
        verify(telegramClient).sendMessage(eq(chatId), contains(link));
        assertEquals(UserState.BASE, dialogManager.getSession(chatId).state());
    }
}

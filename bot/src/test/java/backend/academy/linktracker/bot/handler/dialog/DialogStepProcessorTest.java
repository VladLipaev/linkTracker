package backend.academy.linktracker.bot.handler.dialog;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.client.scrapper.ScrapperRestClient;
import backend.academy.linktracker.bot.client.telegram.TelegramClientFacade;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
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
}

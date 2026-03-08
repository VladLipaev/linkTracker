package backend.academy.linktracker.bot.handler.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.client.scrapper.ScrapperRestClient;
import backend.academy.linktracker.bot.client.telegram.TelegramClientFacade;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@ExtendWith(MockitoExtension.class)
@WireMockTest(httpPort = 54321)
public class DialogStepProcessorIT {
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
    void shouldReturnMessageWhenLinkIsInValid() {
        // Arrange
        long chatId = 123L;
        UserSession session = new UserSession(UserState.WAITING_FOR_TRACK_TAGS, "https://github.com/user/repo");
        dialogManager.setSession(chatId, session);

        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        when(chat.id()).thenReturn(123L);
        when(message.text()).thenReturn(null);
        when(message.chat()).thenReturn(chat);
        when(update.message()).thenReturn(message);
        WireMock.stubFor(WireMock.post(WireMock.urlPathMatching("/links"))
            .withQueryParam("filter", WireMock.equalTo("товар")).willReturn(WireMock.ok("""
                                [
                                    {"id": 1, "title": "товар 1", "details": "описание 1"},
                                    {"id": 2, "title": "товар 2", "details": "описание 2"}
                                ]
                                """).withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
        // Act
        processor.process(update, session);

        // Assert
        verify(telegramClient).sendMessage(eq(chatId), contains("Некорректная ссылка!"));
        assertEquals(
                UserState.WAITING_FOR_TRACK_LINK,
                dialogManager.getSession(chatId).state());
    }
}

package backend.academy.linktracker.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.configuration.TelegramUpdateDispatcher;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.GetMyCommands;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetMyCommandsResponse;
import com.pengrad.telegrambot.response.SendResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
public class TelegramUpdateDispatcherIT {

    @Autowired
    private TelegramUpdateDispatcher telegramUpdateDispatcher;

    @MockitoBean
    private TelegramBot telegramBot;

    @Test
    @DisplayName("Интеграционный тест команды /start")
    public void testStartCommand_CommandIsValid_ReturnsDefaultMessage() {
        SendResponse okResp = mock(SendResponse.class);
        when(okResp.isOk()).thenReturn(true);
        when(telegramBot.execute(any(SendMessage.class))).thenReturn(okResp);

        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.text()).thenReturn("/start");
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);

        // when
        telegramUpdateDispatcher.dispatch(update);

        // then
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot).execute(captor.capture());

        SendMessage sent = captor.getValue();
        var p = sent.getParameters();
        assertEquals(123L, p.get("chat_id"));
        assertEquals("Добро пожаловать в Link Tracker!", p.get("text").toString());
    }

    @Test
    @DisplayName("интеграционный тест команды /help")
    public void testHelpCommand_CommandIsValid_ReturnsDefaultMessage() {
        // given
        SendResponse okResp = mock(SendResponse.class);
        when(okResp.isOk()).thenReturn(true);
        when(telegramBot.execute(any(SendMessage.class))).thenReturn(okResp);

        GetMyCommandsResponse getMyCommandsResponse = mock(GetMyCommandsResponse.class);
        when(telegramBot.execute(any(GetMyCommands.class))).thenReturn(getMyCommandsResponse);
        when(getMyCommandsResponse.commands()).thenReturn(new BotCommand[] {});

        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.text()).thenReturn("/help");
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);

        // when
        telegramUpdateDispatcher.dispatch(update);

        // then
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot, times(2)).execute(captor.capture());

        SendMessage sm = captor.getAllValues().getFirst();
        assertEquals(
                "Выберите интересующую вас команду:",
                sm.getParameters().get("text").toString());
        assertEquals(123L, sm.getParameters().get("chat_id"));
    }

    @Test
    @DisplayName("Интеграционный тест неизвестной команды")
    public void testUnknownCommand_CommandIsUnValid_ReturnDefaultMessage() {
        // given
        SendResponse okResp = mock(SendResponse.class);
        when(okResp.isOk()).thenReturn(true);
        when(telegramBot.execute(any(SendMessage.class))).thenReturn(okResp);

        GetMyCommandsResponse getMyCommandsResponse = mock(GetMyCommandsResponse.class);
        when(telegramBot.execute(any(GetMyCommands.class))).thenReturn(getMyCommandsResponse);
        when(getMyCommandsResponse.commands()).thenReturn(new BotCommand[] {});

        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.text()).thenReturn("шляпа");
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);

        // when
        telegramUpdateDispatcher.dispatch(update);

        // then
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot).execute(captor.capture());

        SendMessage sm = captor.getValue();
        assertEquals(
                "Неизвестная команда. Воспользуйтесь " + "/help, чтобы посмотреть список доступных команд.",
                sm.getParameters().get("text").toString());
        assertEquals(123L, sm.getParameters().get("chat_id"));
    }
}

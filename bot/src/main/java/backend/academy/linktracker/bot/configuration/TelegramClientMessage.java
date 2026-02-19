package backend.academy.linktracker.bot.configuration;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramClientMessage {

    private final TelegramBot telegramBot;

    public void sendMessage(long chatId, String message) {
        SendResponse response = telegramBot.execute(new SendMessage(chatId, message));
        if (!response.isOk()) {
            log.warn("Failed to send message: {} - {}", response.errorCode(), response.description());
        }
    }
}

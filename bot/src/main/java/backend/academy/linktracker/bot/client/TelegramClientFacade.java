package backend.academy.linktracker.bot.client;

import com.pengrad.telegrambot.model.BotCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramClientFacade {

    private final TelegramClientBotCommands telegramClientBotCommands;
    private final TelegramClientMessage telegramClientMessage;

    public BotCommand[] getBotCommands() {
        return telegramClientBotCommands.getBotCommands();
    }

    public void sendMessage(long chatId, String message) {
        telegramClientMessage.sendMessage(chatId, message);
    }
}

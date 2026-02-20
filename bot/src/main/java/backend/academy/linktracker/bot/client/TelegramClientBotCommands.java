package backend.academy.linktracker.bot.client;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.request.GetMyCommands;
import com.pengrad.telegrambot.request.SetMyCommands;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramClientBotCommands {

    private final TelegramBot telegramBot;

    public BotCommand[] getBotCommands() {
        return telegramBot.execute(new GetMyCommands()).commands();
    }

    public void setBotCommands(BotCommand[] botCommands) {
        telegramBot.execute(new SetMyCommands(botCommands));
    }
}

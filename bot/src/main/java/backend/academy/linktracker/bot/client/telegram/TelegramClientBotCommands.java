package backend.academy.linktracker.bot.client.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.botcommandscope.BotCommandScopeAllPrivateChats;
import com.pengrad.telegrambot.request.GetMyCommands;
import com.pengrad.telegrambot.request.SetMyCommands;
import com.pengrad.telegrambot.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramClientBotCommands {

    private final TelegramBot telegramBot;

    public BotCommand[] getBotCommands() {
        return telegramBot.execute(new GetMyCommands()).commands();
    }

    public BaseResponse setBotCommands(BotCommand[] botCommands) {
        SetMyCommands request = new SetMyCommands(botCommands);
        request.scope(new BotCommandScopeAllPrivateChats());
        return telegramBot.execute(request);
    }
}

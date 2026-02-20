package backend.academy.linktracker.bot.handler;

import backend.academy.linktracker.bot.client.TelegramClientFacade;
import com.pengrad.telegrambot.model.Update;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartCommandHandler implements CommandHandler {

    private final TelegramClientFacade telegramClientFacade;

    @Getter
    private final String commandName = "/start";

    @Getter
    private final String description = "Приветствие пользователя";

    @Override
    public void handle(Update update) {
        long chatId = update.message().chat().id();
        telegramClientFacade.sendMessage(chatId, "Добро пожаловать в Link Tracker!");
    }
}

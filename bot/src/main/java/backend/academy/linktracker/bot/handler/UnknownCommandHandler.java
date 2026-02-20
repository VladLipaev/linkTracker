package backend.academy.linktracker.bot.handler;

import backend.academy.linktracker.bot.client.TelegramClientFacade;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UnknownCommandHandler {

    private final TelegramClientFacade telegramClientFacade;

    public void handle(Update update) {
        long chatId = update.message().chat().id();
        telegramClientFacade.sendMessage(
                chatId, "Неизвестная команда. Воспользуйтесь " + "/help, чтобы посмотреть список доступных команд.");
    }
}

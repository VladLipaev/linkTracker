package backend.academy.linktracker.bot.handler;

import backend.academy.linktracker.bot.client.TelegramClientFacade;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.Update;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HelpCommandHandler implements CommandHandler {

    private final TelegramClientFacade telegramClientFacade;

    @Getter
    private final String commandName = "/help";

    @Getter
    private final String description = "Список доступных команд";

    @Override
    public void handle(Update update) {
        long chatId = update.message().chat().id();
        telegramClientFacade.sendMessage(chatId, "Выберите интересующую вас команду:");
        BotCommand[] botCommands = telegramClientFacade.getBotCommands();
        StringBuilder sb = new StringBuilder();
        for (var command : botCommands) {
            sb.append("/")
                    .append(command.command())
                    .append(": ")
                    .append(command.description())
                    .append("\n");
        }
        telegramClientFacade.sendMessage(chatId, sb.toString());
    }
}

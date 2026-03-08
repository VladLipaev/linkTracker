package backend.academy.linktracker.bot.handler;

import backend.academy.linktracker.bot.client.telegram.TelegramClientFacade;
import backend.academy.linktracker.bot.command.TelegramCommand;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class HelpCommandHandler implements CommandHandler {

    private final TelegramClientFacade telegramClientFacade;
    private final TelegramCommand telegramCommand = TelegramCommand.HELP;

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

    @Override
    public String getCommandName() {
        return telegramCommand.getValue();
    }

    @Override
    public String getDescription() {
        return telegramCommand.getDescription();
    }

    @Override
    public boolean isEnabled() {
        return telegramCommand.isEnabled();
    }
}

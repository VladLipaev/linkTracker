package backend.academy.linktracker.bot.handler;

import backend.academy.linktracker.bot.client.scrapper.ScrapperClient;
import backend.academy.linktracker.bot.client.scrapper.ScrapperClientException;
import backend.academy.linktracker.bot.client.telegram.TelegramClientFacade;
import backend.academy.linktracker.bot.command.TelegramCommand;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartCommandHandler implements CommandHandler {

    private final TelegramClientFacade telegramClientFacade;
    private final TelegramCommand telegramCommand = TelegramCommand.START;
    private final ScrapperClient scrapperClient;

    @Override
    public void handle(Update update) {
        long chatId = update.message().chat().id();
        try {
            scrapperClient.registerChat(chatId);

            telegramClientFacade.sendMessage(chatId, "Добро пожаловать в Link Tracker!");
        } catch (ScrapperClientException e) {
            telegramClientFacade.sendMessage(chatId, e.getMessage());
        }
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

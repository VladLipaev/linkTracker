package backend.academy.linktracker.bot.handler;

import backend.academy.linktracker.bot.client.telegram.TelegramClientFacade;
import backend.academy.linktracker.bot.command.TelegramCommand;
import backend.academy.linktracker.bot.handler.dialog.DialogManager;
import backend.academy.linktracker.bot.handler.dialog.UserSession;
import backend.academy.linktracker.bot.handler.dialog.UserState;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListCommandHandler implements CommandHandler {
    private final TelegramClientFacade telegramClientFacade;
    private final DialogManager dialogManager;
    private final TelegramCommand telegramCommand = TelegramCommand.LIST;

    @Override
    public void handle(Update update) {
        long chatId = update.message().chat().id();
        telegramClientFacade.sendMessage(
                chatId,
                "Введите тег, по которому хотите получить"
                + " ссылки, либо введите skip чтобы получить их все");
        dialogManager.setSession(chatId, new UserSession(UserState.WAITING_FOR_LIST_TAG, null));
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

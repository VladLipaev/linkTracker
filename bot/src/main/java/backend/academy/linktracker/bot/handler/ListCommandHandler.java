package backend.academy.linktracker.bot.handler;

import backend.academy.linktracker.bot.client.telegram.TelegramClientFacade;
import backend.academy.linktracker.bot.command.TelegramCommand;
import backend.academy.linktracker.bot.controller.cache.CacheDialogUtil;
import backend.academy.linktracker.bot.handler.dialog.UserSession;
import backend.academy.linktracker.bot.handler.dialog.UserState;
import com.pengrad.telegrambot.model.Update;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListCommandHandler implements CommandHandler {
    private final TelegramClientFacade telegramClientFacade;
    private final TelegramCommand telegramCommand = TelegramCommand.LIST;
    private final CacheDialogUtil cacheDialogUtil;

    @Value("${app.redis.time-to-live}")
    private Duration ttl;

    @Override
    public void handle(Update update) {
        long chatId = update.message().chat().id();
        telegramClientFacade.sendMessage(
                chatId,
                "Введите тег, по которому хотите получить" + " ссылки, либо введите skip чтобы получить их все");
        cacheDialogUtil.addCache(chatId, new UserSession(UserState.WAITING_FOR_LIST_TAG, null), ttl);
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

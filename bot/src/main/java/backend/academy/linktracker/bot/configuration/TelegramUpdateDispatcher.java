package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.handler.CommandHandler;
import backend.academy.linktracker.bot.handler.UnknownCommandHandler;
import com.pengrad.telegrambot.model.Update;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramUpdateDispatcher {

    private final List<CommandHandler> commandHandlers;
    private final UnknownCommandHandler unknownCommandHandler;

    private Map<String, CommandHandler> handlerMap;

    @PostConstruct
    public void initHandlerMap() {
        handlerMap =
                commandHandlers.stream().collect(Collectors.toMap(CommandHandler::getCommandName, handler -> handler));
    }

    public void dispatch(Update update) {
        if (update.message() == null || update.message().text() == null) return;

        String text = update.message().text();
        CommandHandler handler = handlerMap.get(text);

        if (handler != null) {
            handler.handle(update);
        } else {
            unknownCommandHandler.handle(update);
        }
    }
}

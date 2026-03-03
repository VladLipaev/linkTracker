package backend.academy.linktracker.bot.handler;

import backend.academy.linktracker.bot.handler.logging.TelegramDispatcherLogging;
import com.pengrad.telegrambot.model.Update;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramUpdateDispatcher {

    private final List<CommandHandler> commandHandlers;
    private final UnknownCommandHandler unknownCommandHandler;

    private Map<String, CommandHandler> handlerMap;

    public void initHandlerMap() {
        handlerMap = commandHandlers.stream()
            .filter(CommandHandler::isEnabled)
            .collect(Collectors.toMap(CommandHandler::getCommandName, handler -> handler));
    }

    public void dispatch(Update update) {
        if (update == null || update.message() == null || update.message().chat() == null) return;

        Long chatId = update.message().chat().id();
        Integer updateId = update.updateId();
        String text = update.message().text();

        long startTime = System.nanoTime();
        TelegramDispatcherLogging.ReceivedUpdate(updateId, chatId, text);

        try {
            if (text == null) return;

            CommandHandler handler = handlerMap.get(text);
            if (handler == null) {
                unknownCommandHandler.handle(update);
                TelegramDispatcherLogging.UnknownCommandHandled(updateId, chatId, text, startTime);
                return;
            }
            handler.handle(update);
            TelegramDispatcherLogging.CommandHandled(updateId, chatId, startTime, handler);

        } catch (Exception e) {
            TelegramDispatcherLogging.CommandHandlingFailed(updateId, chatId, text, startTime, e);
            throw e;
        }
    }
}

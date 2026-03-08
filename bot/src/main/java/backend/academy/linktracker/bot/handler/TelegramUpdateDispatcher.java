package backend.academy.linktracker.bot.handler;

import backend.academy.linktracker.bot.client.telegram.TelegramClientFacade;
import backend.academy.linktracker.bot.handler.dialog.DialogManager;
import backend.academy.linktracker.bot.handler.dialog.DialogStepProcessor;
import backend.academy.linktracker.bot.handler.dialog.UserSession;
import backend.academy.linktracker.bot.handler.dialog.UserState;
import backend.academy.linktracker.bot.handler.logging.TelegramDispatcherLogging;
import com.pengrad.telegrambot.model.Update;
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
    private final DialogManager dialogManager;
    private final DialogStepProcessor dialogStepProcessor;
    private final TelegramClientFacade telegramClientFacade;

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

            UserSession session = dialogManager.getSession(chatId);

            if (text.startsWith("/")) {
                handleCommand(update, text, session, startTime);
            } else if (session.state() != UserState.BASE) {
                dialogStepProcessor.process(update, session);
            } else {
                unknownCommandHandler.handle(update);
                TelegramDispatcherLogging.UnknownCommandHandled(updateId, chatId, text, startTime);
            }

        } catch (Exception e) {
            TelegramDispatcherLogging.CommandHandlingFailed(updateId, chatId, text, startTime, e);
        }
    }

    private void handleCommand(Update update, String text, UserSession session, Long startTime) {
        long chatId = update.message().chat().id();

        if (text.equals("/cancel")) {
            if (session.state() != UserState.BASE) {
                dialogManager.setSession(chatId, UserSession.base());
                telegramClientFacade.sendMessage(chatId, "Отмена выполнения команды");
            } else {
                telegramClientFacade.sendMessage(chatId, "Нечего отменять");
            }
            return;
        }

        if (session.state() != UserState.BASE) {
            dialogManager.setSession(chatId, UserSession.base());
        }

        CommandHandler handler = handlerMap.get(text);
        if (handler != null) {
            handler.handle(update);
            TelegramDispatcherLogging.CommandHandled(update.updateId(), chatId, startTime, handler);
        } else {
            unknownCommandHandler.handle(update);
            TelegramDispatcherLogging.UnknownCommandHandled(update.updateId(), chatId, text, startTime);
        }
    }
}

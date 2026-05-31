package backend.academy.linktracker.bot.handler;

import backend.academy.linktracker.bot.client.telegram.TelegramClientFacade;
import backend.academy.linktracker.bot.configuration.metrics.BotMetrics;
import backend.academy.linktracker.bot.handler.dialog.DialogManager;
import backend.academy.linktracker.bot.handler.dialog.DialogScrapperStepProcessor;
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
    private final DialogScrapperStepProcessor dialogScrapperStepProcessor;
    private final TelegramClientFacade telegramClientFacade;
    private final BotMetrics botMetrics;

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
        if (text == null) return;
        try {

            UserSession session = dialogManager.getSession(chatId);

            if (text.startsWith("/")) {
                handleCommand(update, text, session, startTime);
            } else if (session.state() != UserState.BASE) {
                dialogScrapperStepProcessor.process(update, session);
            } else {
                unknownCommandHandler.handle(update);
                TelegramDispatcherLogging.UnknownCommandHandled(updateId, chatId, text, startTime);
                botMetrics.incrementCommand("/unknown");
                botMetrics.recordCommandDuration(
                        nanosToMs(System.nanoTime() - startTime), "scrapper_sync_api", "/unknown");
            }

        } catch (Exception e) {
            TelegramDispatcherLogging.CommandHandlingFailed(updateId, chatId, text, startTime, e);
            String command = text.startsWith("/") ? text.split(" ")[0] : "/unknown";
            botMetrics.incrementCommand(command);
            botMetrics.recordCommandDuration(nanosToMs(System.nanoTime() - startTime), "scrapper_sync_api", command);
        }
    }

    private void handleCommand(Update update, String text, UserSession session, Long startTime) {
        long chatId = update.message().chat().id();
        String command = text.split(" ")[0];

        if (text.equals("/cancel")) {
            if (session.state() != UserState.BASE) {
                dialogManager.setSession(chatId, UserSession.base());
                telegramClientFacade.sendMessage(chatId, "Отмена выполнения команды");
            } else {
                telegramClientFacade.sendMessage(chatId, "Нечего отменять");
            }
            botMetrics.incrementCommand("/cancel");
            botMetrics.recordCommandDuration(nanosToMs(System.nanoTime() - startTime), "scrapper_sync_api", "/cancel");
            return;
        }

        if (session.state() != UserState.BASE) {
            dialogManager.setSession(chatId, UserSession.base());
        }

        CommandHandler handler = handlerMap.get(text);
        if (handler != null) {
            long handlerStart = System.nanoTime();
            handler.handle(update);
            long handlerDuration = System.nanoTime() - handlerStart;

            botMetrics.incrementCommand(command);
            botMetrics.recordCommandDuration(nanosToMs(handlerDuration), "scrapper_sync_api", command);
            TelegramDispatcherLogging.CommandHandled(update.updateId(), chatId, startTime, handler);
        } else {
            unknownCommandHandler.handle(update);
            botMetrics.incrementCommand("/unknown");
            botMetrics.recordCommandDuration(nanosToMs(System.nanoTime() - startTime), "scrapper_sync_api", "/unknown");
            TelegramDispatcherLogging.UnknownCommandHandled(update.updateId(), chatId, text, startTime);
        }
    }

    private long nanosToMs(long nanos) {
        return nanos / 1_000_000;
    }
}
